package de.kopfschmerz.protokoll

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.time.LocalDate

class MainActivity : AppCompatActivity() {

    private var entries: List<Entry> = emptyList()
    private lateinit var listView: ListView
    private lateinit var empty: TextView
    private lateinit var summary: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        listView = findViewById(R.id.list)
        empty = findViewById(R.id.empty)
        summary = findViewById(R.id.summary)

        listView.adapter = adapter
        listView.setOnItemClickListener { _, _, position, _ ->
            startActivity(Intent(this, EntryActivity::class.java)
                .putExtra(EntryActivity.EXTRA_ID, entries[position].id))
        }
        findViewById<View>(R.id.btnNew).setOnClickListener {
            startActivity(Intent(this, EntryActivity::class.java))
        }
        findViewById<View>(R.id.btnExport).setOnClickListener { showExportDialog() }
    }

    override fun onResume() {
        super.onResume()
        entries = EntryStore.all(this)
        adapter.notifyDataSetChanged()
        val hasEntries = entries.isNotEmpty()
        listView.visibility = if (hasEntries) View.VISIBLE else View.GONE
        empty.visibility = if (hasEntries) View.GONE else View.VISIBLE

        val since = LocalDate.now().minusDays(29)
        val last30 = entries.filter { !it.startDateTime.toLocalDate().isBefore(since) }
        val days = last30.map { it.startDateTime.toLocalDate() }.distinct().size
        val medDays = last30.filter { it.medication.isNotBlank() }
            .map { it.startDateTime.toLocalDate() }.distinct().size
        summary.text = "Letzte 30 Tage: $days Kopfschmerztage, an $medDays Tagen Medikamente"
    }

    private fun showExportDialog() {
        if (entries.isEmpty()) {
            Toast.makeText(this, "Keine Einträge vorhanden", Toast.LENGTH_SHORT).show()
            return
        }
        val options = arrayOf(
            "PDF – letzte 30 Tage",
            "PDF – letzte 90 Tage",
            "PDF – alle Einträge",
            "CSV (Excel) – alle Einträge",
        )
        AlertDialog.Builder(this)
            .setTitle(R.string.export)
            .setItems(options) { _, which ->
                try {
                    when (which) {
                        0 -> Exporter.sharePdf(this, entries, 30)
                        1 -> Exporter.sharePdf(this, entries, 90)
                        2 -> Exporter.sharePdf(this, entries, null)
                        else -> Exporter.shareCsv(this, entries)
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Export fehlgeschlagen: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
            .show()
    }

    private val adapter = object : BaseAdapter() {
        override fun getCount() = entries.size
        override fun getItem(position: Int) = entries[position]
        override fun getItemId(position: Int) = entries[position].id

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: layoutInflater.inflate(
                android.R.layout.simple_list_item_2, parent, false)
            val e = entries[position]
            view.findViewById<TextView>(android.R.id.text1).text =
                "${e.startDateTime.format(Entry.DATE_TIME)}  ·  Stärke ${e.intensity}/10"
            val details = listOf(e.durationText(), e.location, e.medication)
                .filter { it.isNotBlank() && it != "–" }
            view.findViewById<TextView>(android.R.id.text2).text = details.joinToString("  ·  ")
            return view
        }
    }
}
