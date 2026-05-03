package com.diarioestoico.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.*
import com.diarioestoico.app.MainActivity
import com.diarioestoico.app.data.EntryRepository

class DailyWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = EntryRepository(context)
        val entry = repository.getTodayEntry()

        provideContent {
            GlanceTheme {
                WidgetContent(
                    title = entry?.title ?: "Diário Estoico",
                    quote = entry?.widgetQuote ?: "Abra o app para a leitura de hoje.",
                    author = entry?.author ?: "",
                    day = entry?.day ?: 0,
                    monthName = entry?.monthName ?: ""
                )
            }
        }
    }
}

@Composable
private fun WidgetContent(
    title: String,
    quote: String,
    author: String,
    day: Int,
    monthName: String
) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .clickable(actionStartActivity<MainActivity>())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        contentAlignment = Alignment.TopStart
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.Top
        ) {
            // Date label
            if (day > 0) {
                Text(
                    text = "$day de $monthName".uppercase(),
                    style = TextStyle(
                        color = GlanceTheme.colors.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(GlanceModifier.height(5.dp))
            }

            // Title
            Text(
                text = title,
                style = TextStyle(
                    color = GlanceTheme.colors.onBackground,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 2
            )

            Spacer(GlanceModifier.height(10.dp))

            // Quote — larger and more breathing room
            Text(
                text = "“$quote”",
                style = TextStyle(
                    color = GlanceTheme.colors.onBackground,
                    fontSize = 14.sp,
                    fontStyle = FontStyle.Italic
                ),
                maxLines = 6
            )

            Spacer(GlanceModifier.defaultWeight())

            // Author
            if (author.isNotBlank()) {
                val shortAuthor = author.split(",").firstOrNull()?.trim() ?: author
                Text(
                    text = "— $shortAuthor",
                    style = TextStyle(
                        color = GlanceTheme.colors.secondary,
                        fontSize = 11.sp,
                        fontStyle = FontStyle.Italic
                    ),
                    maxLines = 1
                )
            }
        }
    }
}
