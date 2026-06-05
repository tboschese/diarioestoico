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

    enum class Mode {
        QUOTE_ONLY,       // Just the citation, centered — great for a clean quote post
        REFLECTION_ONLY,  // Just the commentary / reflection text
        FULL              // Quote card + reflection (complete meditation)
    }

    private const val W     = 1080f
    private const val MIN_H = 1350f  // 4:5 minimum; grows for longer content
    private const val PAD   = 72f

    // Colors — always light/parchment so share cards read well everywhere
    private val C_BG      = Color.parseColor("#FAF6EE")
    private val C_INK     = Color.parseColor("#241D15")
    private val C_INK2    = Color.parseColor("#6B5F52")
    private val C_INK3    = Color.parseColor("#A99B85")
    private val C_TINT    = Color.parseColor("#F1E9DA")
    private val C_LINE    = Color.parseColor("#E6DBC9")
    private val C_ACCENT  = Color.parseColor("#7C5230")

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
        val tfReg  = ResourcesCompat.getFont(context, R.font.lora_regular)  ?: Typeface.SERIF
        val tfBold = ResourcesCompat.getFont(context, R.font.lora_bold)     ?: Typeface.create(Typeface.SERIF, Typeface.BOLD)
        val tfItal = ResourcesCompat.getFont(context, R.font.lora_italic)   ?: Typeface.create(Typeface.SERIF, Typeface.ITALIC)

        val cw = W - PAD * 2  // usable content width

        // ── Pre-measure all text layouts ────────────────────────────
        val titlePaint = tp(52f, C_INK, Typeface.DEFAULT_BOLD)
        val titleLayout = sl(entry.title, titlePaint, cw.toInt())

        // Quote layout
        val qInset  = PAD + 20f
        val qWidth  = (W - qInset * 2).toInt()
        val qPaint  = tp(36f, C_INK, tfItal)
        val qLayout = sl(entry.quote, qPaint, qWidth)

        val authorH   = if (entry.author.isNotBlank()) 50f else 0f
        val stripeW   = 4f
        val bPadH     = 40f
        val bPadV     = 36f
        // quote box height: top-pad + " mark + gap + text + author + bottom-pad
        val quoteBoxH = bPadV + 62f + 8f + qLayout.height + authorH + bPadV

        // Commentary layout
        val comPaint  = tp(32f, C_INK, tfReg)
        val comLayout = if (mode != Mode.QUOTE_ONLY && entry.commentary.isNotBlank()) {
            sl(entry.commentary, comPaint, cw.toInt())
        } else null

        // ── Compute total height ─────────────────────────────────────
        var needed = PAD + 20f   // top
        needed += 60f            // header (app name)
        needed += 28f            // divider gap
        needed += 56f            // date
        if (mode != Mode.REFLECTION_ONLY) {
            needed += titleLayout.height + 52f
            needed += 44f        // ornament separator
            needed += quoteBoxH + 48f
        } else {
            needed += titleLayout.height + 40f
        }
        if (comLayout != null) {
            needed += 24f        // label line gap
            needed += 50f        // "REFLEXÃO" label
            needed += comLayout.height + 52f
        }
        needed += 80f + PAD      // branding + bottom

        val finalH = maxOf(needed, MIN_H)

        val bmp = Bitmap.createBitmap(W.toInt(), finalH.toInt(), Bitmap.Config.ARGB_8888)
        val cv  = Canvas(bmp)
        val p   = Paint(Paint.ANTI_ALIAS_FLAG)

        // ── Background ───────────────────────────────────────────────
        p.color = C_BG; p.style = Paint.Style.FILL
        cv.drawRect(0f, 0f, W, finalH, p)

        // ── Thin inner border ────────────────────────────────────────
        p.style = Paint.Style.STROKE; p.strokeWidth = 1.5f
        p.color = C_ACCENT; p.alpha = 55
        cv.drawRect(16f, 16f, W - 16f, finalH - 16f, p)
        p.alpha = 255; p.style = Paint.Style.FILL

        var y = PAD + 20f

        // ── App header ───────────────────────────────────────────────
        cv.drawText(
            "Diário Estoico".uppercase(),
            W / 2f, y + 28f,
            tp(24f, C_ACCENT, tfReg, Paint.Align.CENTER).also { it.letterSpacing = 0.2f }
        )
        y += 56f

        // ── Hairline ─────────────────────────────────────────────────
        p.color = C_LINE; p.style = Paint.Style.STROKE; p.strokeWidth = 1f
        cv.drawLine(PAD + 100f, y, W - PAD - 100f, y, p)
        p.style = Paint.Style.FILL; y += 28f

        // ── Date ─────────────────────────────────────────────────────
        cv.drawText(
            "${entry.day} DE ${entry.monthName.uppercase()}",
            W / 2f, y + 24f,
            tp(26f, C_ACCENT, Typeface.DEFAULT, Paint.Align.CENTER).also { it.letterSpacing = 0.14f }
        )
        y += 56f

        when (mode) {
            Mode.REFLECTION_ONLY -> {
                // Title only (no quote box)
                cv.save(); cv.translate(PAD, y)
                titleLayout.draw(cv); cv.restore()
                y += titleLayout.height + 40f
            }
            else -> {
                // Title
                cv.save(); cv.translate(PAD, y)
                titleLayout.draw(cv); cv.restore()
                y += titleLayout.height + 52f

                // Separator
                cv.drawText("—  ✦  —", W / 2f, y + 14f,
                    tp(20f, C_ACCENT, tfReg, Paint.Align.CENTER))
                y += 44f

                // ── Quote box ────────────────────────────────────────
                val bx0 = PAD - 8f; val bx1 = W - PAD + 8f
                val by0 = y;        val by1 = y + quoteBoxH

                // Tinted background
                p.color = C_TINT
                cv.drawRoundRect(RectF(bx0, by0, bx1, by1), 16f, 16f, p)

                // Left accent stripe
                p.color = C_ACCENT
                cv.drawRoundRect(RectF(bx0, by0 + 24f, bx0 + stripeW, by1 - 24f), 3f, 3f, p)

                // Decorative " mark
                val markPaint = tp(80f, C_ACCENT, tfItal, Paint.Align.LEFT)
                    .also { it.alpha = 80 }
                cv.drawText("“", qInset + bPadH * 0.7f, by0 + bPadV + 50f, markPaint)

                // Quote text
                val qTextY = by0 + bPadV + 66f
                cv.save(); cv.translate(qInset, qTextY); qLayout.draw(cv); cv.restore()

                // Author
                if (entry.author.isNotBlank()) {
                    cv.drawText(
                        "— ${entry.author}".uppercase(),
                        bx1 - bPadH * 0.6f,
                        qTextY + qLayout.height + 30f,
                        tp(22f, C_ACCENT, Typeface.DEFAULT, Paint.Align.RIGHT)
                            .also { it.letterSpacing = 0.12f }
                    )
                }
                y = by1 + 48f
            }
        }

        // ── Commentary / Reflection ──────────────────────────────────
        if (comLayout != null) {
            p.color = C_LINE; p.style = Paint.Style.STROKE; p.strokeWidth = 1f
            cv.drawLine(PAD + 80f, y, W - PAD - 80f, y, p)
            p.style = Paint.Style.FILL; y += 24f

            cv.drawText(
                "REFLEXÃO",
                W / 2f, y + 24f,
                tp(22f, C_INK3, Typeface.DEFAULT, Paint.Align.CENTER)
                    .also { it.letterSpacing = 0.22f }
            )
            y += 50f

            cv.save(); cv.translate(PAD, y); comLayout.draw(cv); cv.restore()
            y += comLayout.height + 52f
        }

        // ── Branding ─────────────────────────────────────────────────
        val rem = finalH - PAD - y
        val brandY = y + rem / 2f

        p.color = C_LINE; p.style = Paint.Style.STROKE; p.strokeWidth = 1f
        cv.drawLine(PAD + 140f, brandY - 10f, W - PAD - 140f, brandY - 10f, p)
        p.style = Paint.Style.FILL

        cv.drawText(
            "366 dias de sabedoria".uppercase(),
            W / 2f, brandY + 20f,
            tp(20f, C_INK3, Typeface.DEFAULT, Paint.Align.CENTER)
                .also { it.letterSpacing = 0.2f; it.alpha = 160 }
        )

        return bmp
    }

    // ── Helpers ───────────────────────────────────────────────────────

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

    private fun sl(text: String, paint: TextPaint, width: Int): StaticLayout =
        StaticLayout.Builder
            .obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(10f, 1f)
            .build()
}
