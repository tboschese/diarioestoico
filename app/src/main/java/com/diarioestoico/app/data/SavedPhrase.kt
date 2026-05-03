package com.diarioestoico.app.data

data class SavedPhrase(
    val text: String,
    val sourceTitle: String,
    val sourceDay: Int,
    val sourceMonth: Int,
    val sourceMonthName: String,
    val savedAt: Long = System.currentTimeMillis()
)
