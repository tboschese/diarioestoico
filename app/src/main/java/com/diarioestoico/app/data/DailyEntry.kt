package com.diarioestoico.app.data

import com.google.gson.annotations.SerializedName

data class DailyEntry(
    @SerializedName("day") val day: Int,
    @SerializedName("month") val month: Int,
    @SerializedName("month_name") val monthName: String,
    @SerializedName("title") val title: String,
    @SerializedName("quote") val quote: String,
    @SerializedName("widget_quote") val widgetQuote: String,
    @SerializedName("author") val author: String,
    @SerializedName("commentary") val commentary: String
)
