package de.kopfschmerz.protokoll

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.slider.Slider
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.time.LocalDateTime
import java.time.ZoneId

class EntryActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ID = "id"
    }

    private var editId: Long? = null
    private var dateTime: LocalDateTime = LocalDateTime.now().withSecond(0).withNano(0)

    private lateinit var btnDate: Button
    private lateinit var btnTime: Button
    private lateinit var intensity: Slider
    private lateinit var intensityValue: TextView
    private lateinit var intensityLabel: TextView
    private lateinit var duration: EditText
    private lateinit var location: ChipGroup
    private lateinit var character: ChipGroup
    private lateinit var nausea: Chip
    private lateinit var light: Chip
    private lateinit var aura: Chip
    private lateinit var medication: EditText
    private lateinit var effect: ChipGroup
    private lateinit var triggers: EditText
    private lateinit var notes: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_entry)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        btnDate = findViewById(R.id.btnDate)
        btnTime = findViewById(R.id.btnTime)
        intensity = findViewById(R.id.intensity)
        intensityValue = findViewById(R.id.intensityValue)
        intensityLabel = findViewById(R.id.intensityLabel)
        duration = findViewById(R.id.duration)
        location = findViewById(R.id.location)
        character = findViewById(R.id.character)
        nausea = findViewById(R.id.nausea)
        light = findViewById(R.id.light)
        aura = findViewById(R.id.aura)
        medication = findViewById(R.id.medication)
        effect = findViewById(R.id.effect)
        triggers = findViewById(R.id.triggers)
        notes = findViewById(R.id.notes)

        addChoices(location, resources.getStringArray(R.array.locations).toList())
        addChoices(character, resources.getStringArray(R.array.characters).toList())
        // erster Eintrag ("–") = keine Angabe → kein Chip
        addChoices(effect, resources.getStringArray(R.array.effects).drop(1))
        (location.getChildAt(0) as Chip).isChecked = true
        (character.getChildAt(0) as Chip).isChecked = true

        intensity.addOnChangeListener { _, _, _ -> updateIntensityLabel() }

        val id = intent.getLongExtra(EXTRA_ID, -1L)
        if (id != -1L) {
            EntryStore.get(this, id)?.let { fill(it) }
        }

        btnDate.setOnClickListener {
            DatePickerDialog(this, { _, y, m, d ->
                dateTime = dateTime.withYear(y).withMonth(m + 1).withDayOfMonth(d)
                updateDateTimeButtons()
            }, dateTime.year, dateTime.monthValue - 1, dateTime.dayOfMonth).show()
        }
        btnTime.setOnClickListener {
            TimePickerDialog(this, { _, h, min ->
                dateTime = dateTime.withHour(h).withMinute(min)
                updateDateTimeButtons()
            }, dateTime.hour, dateTime.minute, true).show()
        }
        findViewById<Button>(R.id.btnSave).setOnClickListener { save() }
        findViewById<Button>(R.id.btnDelete).setOnClickListener { confirmDelete() }

        updateDateTimeButtons()
        updateIntensityLabel()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun fill(e: Entry) {
        editId = e.id
        dateTime = e.startDateTime
        intensity.value = e.intensity.coerceIn(1, 10).toFloat()
        if (e.durationHours > 0) duration.setText(e.durationHours.toString().replace('.', ','))
        selectChip(location, e.location)
        selectChip(character, e.character)
        selectChip(effect, e.effect)
        nausea.isChecked = e.nausea
        light.isChecked = e.lightSensitive
        aura.isChecked = e.aura
        medication.setText(e.medication)
        triggers.setText(e.triggers)
        notes.setText(e.notes)
        findViewById<Button>(R.id.btnDelete).visibility = android.view.View.VISIBLE
    }

    private fun addChoices(group: ChipGroup, items: List<String>) {
        for (item in items) {
            val chip = layoutInflater.inflate(R.layout.item_choice_chip, group, false) as Chip
            chip.id = android.view.View.generateViewId()
            chip.text = item
            group.addView(chip)
        }
    }

    private fun selectChip(group: ChipGroup, value: String) {
        for (i in 0 until group.childCount) {
            val chip = group.getChildAt(i) as Chip
            if (chip.text.toString() == value) {
                chip.isChecked = true
                return
            }
        }
    }

    private fun selectedText(group: ChipGroup, fallback: String): String =
        group.findViewById<Chip>(group.checkedChipId)?.text?.toString() ?: fallback

    private fun updateDateTimeButtons() {
        btnDate.text = dateTime.format(Entry.DATE)
        btnTime.text = dateTime.format(Entry.TIME) + " Uhr"
    }

    private fun updateIntensityLabel() {
        val v = intensity.value.toInt()
        val word = when {
            v <= 3 -> "leicht"
            v <= 6 -> "mittel"
            v <= 8 -> "stark"
            else -> "sehr stark"
        }
        intensityValue.text = v.toString()
        intensityLabel.text = "/ 10 · $word"
    }

    private fun save() {
        val durationText = duration.text.toString().trim().replace(',', '.')
        val hours = if (durationText.isEmpty()) 0.0 else durationText.toDoubleOrNull()
        if (hours == null || hours < 0) {
            duration.error = "Bitte eine Zahl eingeben"
            return
        }
        val entry = Entry(
            id = editId ?: System.currentTimeMillis(),
            start = dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            durationHours = hours,
            intensity = intensity.value.toInt(),
            location = selectedText(location, ""),
            character = selectedText(character, ""),
            nausea = nausea.isChecked,
            lightSensitive = light.isChecked,
            aura = aura.isChecked,
            medication = medication.text.toString().trim(),
            effect = selectedText(effect, "–"),
            triggers = triggers.text.toString().trim(),
            notes = notes.text.toString().trim(),
        )
        EntryStore.save(this, entry)
        Toast.makeText(this, "Gespeichert", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun confirmDelete() {
        val id = editId ?: return
        AlertDialog.Builder(this)
            .setMessage("Diesen Eintrag wirklich löschen?")
            .setPositiveButton(R.string.delete) { _, _ ->
                EntryStore.delete(this, id)
                finish()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
