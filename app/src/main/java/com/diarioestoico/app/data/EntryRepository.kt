package com.diarioestoico.app.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDate

class EntryRepository(private val context: Context) {

    private val entries: List<DailyEntry> by lazy { loadEntries() }

    private fun loadEntries(): List<DailyEntry> {
        val json = context.assets.open("entries.json").bufferedReader().readText()
        val type = object : TypeToken<List<DailyEntry>>() {}.type
        return Gson().fromJson(json, type)
    }

    fun getTodayEntry(): DailyEntry? {
        val today = LocalDate.now()
        return getEntry(today.dayOfMonth, today.monthValue)
    }

    fun getEntry(day: Int, month: Int): DailyEntry? {
        return entries.find { it.day == day && it.month == month }
    }

    fun getAllEntries(): List<DailyEntry> = entries
}
