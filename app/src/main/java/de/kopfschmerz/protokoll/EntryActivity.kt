package de.kopfschmerz.protokoll

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
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
    private lateinit var intensity: SeekBar
    private lateinit var intensityLabel: TextView
    private lateinit var duration: EditText
    private lateinit var location: Spinner
    private lateinit var character: Spinner
    private lateinit var nausea: CheckBox
    private lateinit var light: CheckBox
    private lateinit var aura: CheckBox
    private lateinit var medication: EditText
    private lateinit var effect: Spinner
    private lateinit var triggers: EditText
    private lateinit var notes: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_entry)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        btnDate = findViewById(R.id.btnDate)
        btnTime = findViewById(R.id.btnTime)
        intensity = findViewById(R.id.intensity)
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

        intensity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, value: Int, fromUser: Boolean) = updateIntensityLabel()
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })

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
        intensity.progress = e.intensity
        if (e.durationHours > 0) duration.setText(e.durationHours.toString().replace('.', ','))
        selectSpinner(location, e.location)
        selectSpinner(character, e.character)
        selectSpinner(effect, e.effect)
        nausea.isChecked = e.nausea
        light.isChecked = e.lightSensitive
        aura.isChecked = e.aura
        medication.setText(e.medication)
        triggers.setText(e.triggers)
        notes.setText(e.notes)
        findViewById<Button>(R.id.btnDelete).visibility = android.view.View.VISIBLE
    }

    private fun selectSpinner(spinner: Spinner, value: String) {
        for (i in 0 until spinner.count) {
            if (spinner.getItemAtPosition(i).toString() == value) {
                spinner.setSelection(i)
                return
            }
        }
    }

    private fun updateDateTimeButtons() {
        btnDate.text = dateTime.format(Entry.DATE)
        btnTime.text = dateTime.format(Entry.TIME) + " Uhr"
    }

    private fun updateIntensityLabel() {
        val v = intensity.progress
        val word = when {
            v <= 3 -> "leicht"
            v <= 6 -> "mittel"
            v <= 8 -> "stark"
            else -> "sehr stark"
        }
        intensityLabel.text = "Stärke: $v / 10 ($word)"
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
            intensity = intensity.progress,
            location = location.selectedItem.toString(),
            character = character.selectedItem.toString(),
            nausea = nausea.isChecked,
            lightSensitive = light.isChecked,
            aura = aura.isChecked,
            medication = medication.text.toString().trim(),
            effect = effect.selectedItem.toString(),
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
