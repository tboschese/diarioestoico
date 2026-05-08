package com.diarioestoico.app.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.diarioestoico.app.R
import com.diarioestoico.app.data.DailyEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object ShareCardGenerator {

    private const val W    = 1080f
    private const val H    = 1350f   // 4:5 — funciona em Instagram, WhatsApp, Stories
    private const val PAD  = 72f

    suspend fun shareEntry(context: Context, entry: DailyEntry) = withContext(Dispatchers.IO) {
        val bitmap = buildCard(context, entry)

        val file = File(context.cacheDir, "share/diario_estoico.png")
        file.parentFile?.mkdirs()
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 95, it) }
        bitmap.recycle()

        val uri = FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file
        )

        withContext(Dispatchers.Main) {
            context.startActivity(
                Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type  = "image/png"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    },
                    "Compartilhar meditação"
                )
            )
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Card drawing
    // ─────────────────────────────────────────────────────────────────

    private fun buildCard(context: Context, entry: DailyEntry): Bitmap {
        val bmp = Bitmap.createBitmap(W.toInt(), H.toInt(), Bitmap.Config.ARGB_8888)
        val cv  = Canvas(bmp)

        // ── Palette ────────────────────────────────────────────────
        val bgColor      = Color.parseColor("#FAF8F4")
        val accentColor  = Color.parseColor("#7A5C1E")
        val inkColor     = Color.parseColor("#1A1614")
        val inkLightClr  = Color.parseColor("#6B5F56")
        val quoteBoxClr  = Color.parseColor("#EEE8DE")
        val dividerClr   = Color.parseColor("#D9D0C4")

        // ── Typefaces ──────────────────────────────────────────────
        val tfReg  = ResourcesCompat.getFont(context, R.font.lora_regular)
            ?: Typeface.SERIF
        val tfBold = ResourcesCompat.getFont(context, R.font.lora_bold)
            ?: Typeface.create(Typeface.SERIF, Typeface.BOLD)
        val tfItal = ResourcesCompat.getFont(context, R.font.lora_italic)
            ?: Typeface.create(Typeface.SERIF, Typeface.ITALIC)

        val p = Paint(Paint.ANTI_ALIAS_FLAG)

        // ── Background ─────────────────────────────────────────────
        p.color = bgColor
        cv.drawRect(0f, 0f, W, H, p)

        // ── Thin decorative border ─────────────────────────────────
        p.style = Paint.Style.STROKE
        p.strokeWidth = 2f
        p.color = accentColor; p.alpha = 30
        cv.drawRect(20f, 20f, W - 20f, H - 20f, p)
        p.alpha = 255; p.style = Paint.Style.FILL

        val cw = W - PAD * 2     // usable content width
        var y  = PAD + 20f

        // ── 1. App header ──────────────────────────────────────────
        val headerPaint = tp(30f, accentColor, tfReg, Paint.Align.CENTER)
            .also { it.letterSpacing = 0.18f }
        cv.drawText("✦  DIÁRIO ESTOICO  ✦", W / 2f, y + 30f, headerPaint)
        y += 60f

        // ── 2. Hairline divider ────────────────────────────────────
        p.color = dividerClr; p.style = Paint.Style.STROKE; p.strokeWidth = 1f
        cv.drawLine(PAD + 80f, y, W - PAD - 80f, y, p)
        p.style = Paint.Style.FILL; y += 28f

        // ── 3. Date ────────────────────────────────────────────────
        val datePaint = tp(28f, accentColor, tfReg, Paint.Align.CENTER)
            .also { it.letterSpacing = 0.12f }
        cv.drawText("${entry.day} DE ${entry.monthName.uppercase()}", W / 2f, y + 28f, datePaint)
        y += 60f

        // ── 4. Title ───────────────────────────────────────────────
        val titleText = entry.title.take(80)
        val titlePaint = tp(58f, inkColor, tfBold)
        val titleLayout = StaticLayout.Builder
            .obtain(titleText, 0, titleText.length, titlePaint, cw.toInt())
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(10f, 1f)
            .build()
        cv.save(); cv.translate(PAD, y); titleLayout.draw(cv); cv.restore()
        y += titleLayout.height + 52f

        // ── 5. Ornament ────────────────────────────────────────────
        cv.drawText("—  ✦  —", W / 2f, y + 16f, tp(22f, accentColor, tfReg, Paint.Align.CENTER))
        y += 50f

        // ── 6. Quote box ───────────────────────────────────────────
        val quoteText = entry.quote.let {
            if (it.length > 220) "${it.take(217)}…" else it
        }
        val authorText = if (entry.author.isNotBlank()) "— ${entry.author}" else ""

        // Layout geometry
        val bx0  = PAD - 10f
        val bx1  = W - PAD + 10f
        val bPad = 40f

        val qPaint = tp(38f, inkColor, tfItal)
        val qWidth = (bx1 - bx0 - bPad * 2 - 4f).toInt()
        val qLayout = StaticLayout.Builder
            .obtain(quoteText, 0, quoteText.length, qPaint, qWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(8f, 1f)
            .build()

        val authorH = if (authorText.isNotEmpty()) 52f else 0f
        // from box-top: bPad (36 top) + 68 (mark area) + qLayout + authorH + bPad
        val boxH = bPad + 68f + qLayout.height.toFloat() + authorH + 20f + bPad
        val by0  = y
        val by1  = y + boxH

        // Box fill
        p.color = quoteBoxClr
        cv.drawRoundRect(RectF(bx0, by0, bx1, by1), 14f, 14f, p)

        // Opening quote mark " (decorative, semi-transparent)
        val markPaint = tp(88f, accentColor, tfBold, Paint.Align.LEFT)
            .also { it.alpha = 160 }
        cv.drawText("“", bx0 + bPad, by0 + bPad + 60f, markPaint)

        // Quote text (starts just below the mark's cap-height)
        val qTextY = by0 + bPad + 72f
        cv.save(); cv.translate(bx0 + bPad, qTextY); qLayout.draw(cv); cv.restore()

        // Author attribution
        if (authorText.isNotEmpty()) {
            cv.drawText(
                authorText,
                bx1 - bPad,
                qTextY + qLayout.height + 36f,
                tp(28f, accentColor, tfReg, Paint.Align.RIGHT)
            )
        }

        y = by1 + 52f

        // ── 7. Branding centered in remaining space ────────────────
        val remaining = H - PAD - y
        val brandY    = y + remaining / 2f

        p.color = dividerClr; p.style = Paint.Style.STROKE; p.strokeWidth = 1f
        cv.drawLine(PAD + 120f, brandY - 12f, W - PAD - 120f, brandY - 12f, p)

        cv.drawText(
            "Diário Estoico",
            W / 2f,
            brandY + 22f,
            tp(26f, inkLightClr, tfItal, Paint.Align.CENTER).also { it.alpha = 150 }
        )

        return bmp
    }

    /** Convenience — creates a TextPaint with common settings. */
    private fun tp(
        size: Float,
        color: Int,
        face: Typeface,
        align: Paint.Align = Paint.Align.LEFT
    ) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize  = size
        this.color = color
        typeface  = face
        textAlign = align
    }
}
