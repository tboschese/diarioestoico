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

    enum class Mode { QUOTE_ONLY, QUOTE_WITH_COMMENTARY }

    private const val W       = 1080f
    private const val MIN_H   = 1350f   // 4:5 mínimo; cresce conforme conteúdo
    private const val PAD     = 72f

    suspend fun shareEntry(
        context: Context,
        entry: DailyEntry,
        mode: Mode = Mode.QUOTE_ONLY
    ) = withContext(Dispatchers.IO) {
        val bitmap = buildCard(context, entry, mode)

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

    private fun buildCard(context: Context, entry: DailyEntry, mode: Mode): Bitmap {
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

        val cw = W - PAD * 2     // usable content width

        // ── Pre-measure layouts so o card cresce sem cortar texto ──
        val titlePaint = tp(58f, inkColor, tfBold)
        val titleLayout = StaticLayout.Builder
            .obtain(entry.title, 0, entry.title.length, titlePaint, cw.toInt())
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(10f, 1f)
            .build()

        val bx0  = PAD - 10f
        val bx1  = W - PAD + 10f
        val bPad = 40f
        val qPaint = tp(38f, inkColor, tfItal)
        val qWidth = (bx1 - bx0 - bPad * 2 - 4f).toInt()
        val qLayout = StaticLayout.Builder
            .obtain(entry.quote, 0, entry.quote.length, qPaint, qWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(8f, 1f)
            .build()
        val authorText = if (entry.author.isNotBlank()) "— ${entry.author}" else ""
        val authorH = if (authorText.isNotEmpty()) 52f else 0f
        val boxH = bPad + 68f + qLayout.height + authorH + 20f + bPad

        val commentaryPaint = tp(30f, inkColor, tfReg)
        val commentaryLayout = if (mode == Mode.QUOTE_WITH_COMMENTARY && entry.commentary.isNotBlank()) {
            StaticLayout.Builder
                .obtain(entry.commentary, 0, entry.commentary.length, commentaryPaint, cw.toInt())
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(10f, 1f)
                .build()
        } else null

        // ── Compute total height ──────────────────────────────────
        var needed = PAD + 20f       // top
        needed += 60f                // header
        needed += 28f                // divider gap
        needed += 60f                // date
        needed += titleLayout.height + 52f
        needed += 50f                // ornament
        needed += boxH + 52f
        if (commentaryLayout != null) {
            needed += 30f            // divider
            needed += 50f            // "REFLEXÃO" label
            needed += commentaryLayout.height + 52f
        }
        needed += 100f               // branding area
        needed += PAD                // bottom

        val finalH = maxOf(needed, MIN_H)

        val bmp = Bitmap.createBitmap(W.toInt(), finalH.toInt(), Bitmap.Config.ARGB_8888)
        val cv  = Canvas(bmp)
        val p   = Paint(Paint.ANTI_ALIAS_FLAG)

        // ── Background ─────────────────────────────────────────────
        p.color = bgColor
        cv.drawRect(0f, 0f, W, finalH, p)

        // ── Thin decorative border ─────────────────────────────────
        p.style = Paint.Style.STROKE
        p.strokeWidth = 2f
        p.color = accentColor; p.alpha = 30
        cv.drawRect(20f, 20f, W - 20f, finalH - 20f, p)
        p.alpha = 255; p.style = Paint.Style.FILL

        var y = PAD + 20f

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
        cv.save(); cv.translate(PAD, y); titleLayout.draw(cv); cv.restore()
        y += titleLayout.height + 52f

        // ── 5. Ornament ────────────────────────────────────────────
        cv.drawText("—  ✦  —", W / 2f, y + 16f, tp(22f, accentColor, tfReg, Paint.Align.CENTER))
        y += 50f

        // ── 6. Quote box ───────────────────────────────────────────
        val by0  = y
        val by1  = y + boxH

        p.color = quoteBoxClr
        cv.drawRoundRect(RectF(bx0, by0, bx1, by1), 14f, 14f, p)

        val markPaint = tp(88f, accentColor, tfBold, Paint.Align.LEFT)
            .also { it.alpha = 160 }
        cv.drawText("“", bx0 + bPad, by0 + bPad + 60f, markPaint)

        val qTextY = by0 + bPad + 72f
        cv.save(); cv.translate(bx0 + bPad, qTextY); qLayout.draw(cv); cv.restore()

        if (authorText.isNotEmpty()) {
            cv.drawText(
                authorText,
                bx1 - bPad,
                qTextY + qLayout.height + 36f,
                tp(28f, accentColor, tfReg, Paint.Align.RIGHT)
            )
        }

        y = by1 + 52f

        // ── 7. Commentary (optional) ───────────────────────────────
        if (commentaryLayout != null) {
            p.color = dividerClr; p.style = Paint.Style.STROKE; p.strokeWidth = 1f
            cv.drawLine(PAD + 80f, y, W - PAD - 80f, y, p)
            p.style = Paint.Style.FILL
            y += 30f

            cv.drawText(
                "REFLEXÃO",
                W / 2f,
                y + 26f,
                tp(26f, accentColor, tfReg, Paint.Align.CENTER).also { it.letterSpacing = 0.22f }
            )
            y += 50f

            cv.save(); cv.translate(PAD, y); commentaryLayout.draw(cv); cv.restore()
            y += commentaryLayout.height + 52f
        }

        // ── 8. Branding centered in remaining space ────────────────
        val remaining = finalH - PAD - y
        val brandY    = y + remaining / 2f

        p.color = dividerClr; p.style = Paint.Style.STROKE; p.strokeWidth = 1f
        cv.drawLine(PAD + 120f, brandY - 12f, W - PAD - 120f, brandY - 12f, p)
        p.style = Paint.Style.FILL

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
