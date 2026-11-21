package de.kopfschmerz.protokoll

import org.json.JSONObject
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class Entry(
    val id: Long,
    val start: Long,            // Beginn, Epoch-Millisekunden
    val durationHours: Double,
    val intensity: Int,         // 1..10
    val location: String,
    val character: String,
    val nausea: Boolean,
    val lightSensitive: Boolean,
    val aura: Boolean,
    val medication: String,
    val effect: String,
    val triggers: String,
    val notes: String,
) {
    val startDateTime: LocalDateTime
        get() = LocalDateTime.ofInstant(Instant.ofEpochMilli(start), ZoneId.systemDefault())

    fun symptomsText(): String = listOfNotNull(
        if (nausea) "Übelkeit" else null,
        if (lightSensitive) "Licht/Lärm" else null,
        if (aura) "Aura" else null,
    ).joinToString(", ")

    fun durationText(): String =
        if (durationHours <= 0.0) "–"
        else (if (durationHours % 1.0 == 0.0) durationHours.toInt().toString()
              else String.format(java.util.Locale.GERMANY, "%.1f", durationHours)) + " h"

    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("start", start)
        .put("durationHours", durationHours)
        .put("intensity", intensity)
        .put("location", location)
        .put("character", character)
        .put("nausea", nausea)
        .put("lightSensitive", lightSensitive)
        .put("aura", aura)
        .put("medication", medication)
        .put("effect", effect)
        .put("triggers", triggers)
        .put("notes", notes)

    companion object {
        val DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        val DATE_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("EE dd.MM.yyyy, HH:mm", java.util.Locale.GERMANY)

        fun fromJson(o: JSONObject) = Entry(
            id = o.getLong("id"),
            start = o.getLong("start"),
            durationHours = o.optDouble("durationHours", 0.0),
            intensity = o.optInt("intensity", 5),
            location = o.optString("location"),
            character = o.optString("character"),
            nausea = o.optBoolean("nausea"),
            lightSensitive = o.optBoolean("lightSensitive"),
            aura = o.optBoolean("aura"),
            medication = o.optString("medication"),
            effect = o.optString("effect"),
            triggers = o.optString("triggers"),
            notes = o.optString("notes"),
        )
    }
}
