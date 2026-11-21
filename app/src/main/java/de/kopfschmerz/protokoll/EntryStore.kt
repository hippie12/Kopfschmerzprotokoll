package de.kopfschmerz.protokoll

import android.content.Context
import org.json.JSONArray

/** Speichert alle Einträge lokal auf dem Gerät (JSON in SharedPreferences). */
object EntryStore {
    private const val PREFS = "kopfschmerzprotokoll"
    private const val KEY = "entries"

    fun all(context: Context): List<Entry> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
            ?: return emptyList()
        val arr = JSONArray(raw)
        return (0 until arr.length()).map { Entry.fromJson(arr.getJSONObject(it)) }
            .sortedByDescending { it.start }
    }

    fun get(context: Context, id: Long): Entry? = all(context).firstOrNull { it.id == id }

    fun save(context: Context, entry: Entry) {
        val list = all(context).filter { it.id != entry.id } + entry
        write(context, list)
    }

    fun delete(context: Context, id: Long) {
        write(context, all(context).filter { it.id != id })
    }

    private fun write(context: Context, list: List<Entry>) {
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, arr.toString()).apply()
    }
}
