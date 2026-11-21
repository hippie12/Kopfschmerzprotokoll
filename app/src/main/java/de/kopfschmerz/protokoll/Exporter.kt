package de.kopfschmerz.protokoll

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.util.Locale

object Exporter {

    // ---------- Teilen ----------

    fun sharePdf(context: Context, all: List<Entry>, lastDays: Int?) {
        val entries = filter(all, lastDays).sortedBy { it.start }
        val file = File(exportDir(context), "Kopfschmerzprotokoll_${LocalDate.now()}.pdf")
        writePdf(file, entries, lastDays)
        share(context, file, "application/pdf")
    }

    fun shareCsv(context: Context, all: List<Entry>) {
        val file = File(exportDir(context), "Kopfschmerzprotokoll_${LocalDate.now()}.csv")
        writeCsv(file, all.sortedBy { it.start })
        share(context, file, "text/csv")
    }

    private fun filter(all: List<Entry>, lastDays: Int?): List<Entry> {
        if (lastDays == null) return all
        val since = LocalDate.now().minusDays((lastDays - 1).toLong())
        return all.filter { !it.startDateTime.toLocalDate().isBefore(since) }
    }

    private fun exportDir(context: Context): File =
        File(context.cacheDir, "exports").apply { mkdirs() }

    private fun share(context: Context, file: File, mime: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Kopfschmerzprotokoll")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "Protokoll teilen / speichern"))
    }

    // ---------- CSV ----------

    private fun writeCsv(file: File, entries: List<Entry>) {
        fun esc(s: String) = "\"" + s.replace("\"", "\"\"") + "\""
        val sb = StringBuilder("﻿") // BOM, damit Excel Umlaute richtig anzeigt
        sb.append(listOf("Datum", "Uhrzeit", "Dauer (h)", "Stärke", "Ort", "Art", "Übelkeit",
            "Licht/Lärm", "Aura", "Medikament", "Wirkung", "Auslöser", "Notizen")
            .joinToString(";") { esc(it) }).append("\r\n")
        for (e in entries) {
            val dt = e.startDateTime
            sb.append(listOf(
                dt.format(Entry.DATE), dt.format(Entry.TIME),
                if (e.durationHours > 0) e.durationHours.toString().replace('.', ',') else "",
                e.intensity.toString(), e.location, e.character,
                if (e.nausea) "ja" else "nein",
                if (e.lightSensitive) "ja" else "nein",
                if (e.aura) "ja" else "nein",
                e.medication, e.effect, e.triggers, e.notes,
            ).joinToString(";") { esc(it) }).append("\r\n")
        }
        file.writeText(sb.toString(), Charsets.UTF_8)
    }

    // ---------- PDF ----------

    private const val PAGE_W = 595 // A4 in Punkten
    private const val PAGE_H = 842
    private const val MARGIN = 36f

    private class Column(val title: String, val width: Float, val value: (Entry) -> String)

    private val columns = listOf(
        Column("Datum / Beginn", 78f) { it.startDateTime.format(Entry.DATE_TIME) },
        Column("Dauer", 34f) { it.durationText() },
        Column("Stärke", 34f) { "${it.intensity}/10" },
        Column("Ort / Art", 86f) { "${it.location}\n${it.character}" },
        Column("Begleit-\nsymptome", 62f) { it.symptomsText() },
        Column("Medikament / Wirkung", 104f) {
            if (it.medication.isBlank()) "–"
            else it.medication + if (it.effect.isNotBlank() && it.effect != "–") "\n(${it.effect})" else ""
        },
        Column("Auslöser / Notizen", 125f) {
            listOf(it.triggers, it.notes).filter { s -> s.isNotBlank() }.joinToString("\n")
        },
    )

    private fun writePdf(file: File, entries: List<Entry>, lastDays: Int?) {
        val doc = PdfDocument()
        val w = PdfWriter(doc)

        // Kopf
        w.newPage()
        w.text("Kopfschmerzprotokoll", w.title, 22f)
        val period = when {
            entries.isEmpty() -> "keine Einträge"
            else -> entries.first().startDateTime.format(Entry.DATE) + " – " +
                    entries.last().startDateTime.format(Entry.DATE)
        }
        val label = if (lastDays != null) "Zeitraum (letzte $lastDays Tage)" else "Zeitraum"
        w.text("$label: $period", w.normal, 14f)
        w.text("Erstellt am ${LocalDate.now().format(Entry.DATE)}", w.normal, 14f)
        w.y += 6f

        // Zusammenfassung
        val days = entries.map { it.startDateTime.toLocalDate() }.distinct().size
        val medDays = entries.filter { it.medication.isNotBlank() }
            .map { it.startDateTime.toLocalDate() }.distinct().size
        val avg = if (entries.isEmpty()) 0.0 else entries.map { it.intensity }.average()
        val avgDur = entries.filter { it.durationHours > 0 }.map { it.durationHours }
            .let { if (it.isEmpty()) null else it.average() }
        w.text("Zusammenfassung", w.bold, 14f)
        w.text("Einträge: ${entries.size}   ·   Kopfschmerztage: $days   ·   Tage mit Medikamenten: $medDays", w.normal, 13f)
        w.text(String.format(Locale.GERMANY, "Durchschnittliche Stärke: %.1f / 10", avg) +
            (avgDur?.let { String.format(Locale.GERMANY, "   ·   Durchschnittliche Dauer: %.1f h", it) } ?: ""),
            w.normal, 13f)
        w.y += 10f

        // Tabelle
        w.tableHeader()
        entries.forEachIndexed { i, e -> w.row(e, shaded = i % 2 == 1) }

        w.finish()
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
    }

    private class PdfWriter(val doc: PdfDocument) {
        val title = Paint().apply { textSize = 18f; isFakeBoldText = true; isAntiAlias = true }
        val normal = Paint().apply { textSize = 10f; isAntiAlias = true }
        val bold = Paint(normal).apply { isFakeBoldText = true }
        val cell = Paint().apply { textSize = 8f; isAntiAlias = true }
        val cellBold = Paint(cell).apply { isFakeBoldText = true }
        val line = Paint().apply { color = Color.LTGRAY; strokeWidth = 0.5f }
        val shade = Paint().apply { color = Color.rgb(242, 244, 250) }
        val headerBg = Paint().apply { color = Color.rgb(220, 225, 245) }
        val footer = Paint().apply { textSize = 8f; color = Color.GRAY; isAntiAlias = true }

        private var page: PdfDocument.Page? = null
        private var pageNo = 0
        lateinit var canvas: Canvas
        var y = MARGIN

        private val lineH = 10f
        private val pad = 3f

        fun newPage() {
            page?.let { doc.finishPage(it) }
            pageNo++
            val p = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNo).create())
            page = p
            canvas = p.canvas
            canvas.drawText("Seite $pageNo", PAGE_W - MARGIN - 30f, PAGE_H - 18f, footer)
            canvas.drawText("Kopfschmerzprotokoll", MARGIN, PAGE_H - 18f, footer)
            y = MARGIN
        }

        fun finish() {
            page?.let { doc.finishPage(it) }
            page = null
        }

        fun text(s: String, paint: Paint, height: Float) {
            y += height
            canvas.drawText(s, MARGIN, y, paint)
        }

        fun tableHeader() {
            val cells = columns.map { wrap(it.title, cellBold, it.width - 2 * pad) }
            val h = cells.maxOf { it.size } * lineH + 2 * pad
            canvas.drawRect(MARGIN, y, PAGE_W - MARGIN, y + h, headerBg)
            drawCells(cells, cellBold)
            y += h
        }

        fun row(e: Entry, shaded: Boolean) {
            val cells = columns.map { wrap(it.value(e), cell, it.width - 2 * pad) }
            val h = cells.maxOf { it.size } * lineH + 2 * pad
            if (y + h > PAGE_H - MARGIN - 10f) {
                newPage()
                tableHeader()
            }
            if (shaded) canvas.drawRect(MARGIN, y, PAGE_W - MARGIN, y + h, shade)
            drawCells(cells, cell)
            y += h
            canvas.drawLine(MARGIN, y, PAGE_W - MARGIN, y, line)
        }

        private fun drawCells(cells: List<List<String>>, paint: Paint) {
            var x = MARGIN
            cells.forEachIndexed { i, lines ->
                lines.forEachIndexed { li, l ->
                    canvas.drawText(l, x + pad, y + pad + (li + 1) * lineH - 2f, paint)
                }
                x += columns[i].width
            }
        }

        /** Bricht Text in Zeilen um, die in [maxWidth] passen. */
        private fun wrap(text: String, paint: Paint, maxWidth: Float): List<String> {
            val out = mutableListOf<String>()
            for (paragraph in text.split("\n")) {
                var current = ""
                for (word in paragraph.split(" ").filter { it.isNotEmpty() }) {
                    val candidate = if (current.isEmpty()) word else "$current $word"
                    if (paint.measureText(candidate) <= maxWidth) {
                        current = candidate
                    } else {
                        if (current.isNotEmpty()) out.add(current)
                        // zu lange Einzelwörter hart umbrechen
                        var rest = word
                        while (paint.measureText(rest) > maxWidth && rest.length > 1) {
                            var n = rest.length - 1
                            while (n > 1 && paint.measureText(rest.substring(0, n)) > maxWidth) n--
                            out.add(rest.substring(0, n))
                            rest = rest.substring(n)
                        }
                        current = rest
                    }
                }
                out.add(current)
            }
            return if (out.isEmpty()) listOf("") else out
        }
    }
}
