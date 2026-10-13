package app.mediapicker

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

/** One line of the file browser: thumbnail or type icon, name, size and date, selection mark. */
class FileRow(c: Context) : LinearLayout(c) {
    val thumb = ImageView(c)
    private val title = TextView(c)
    private val sub = TextView(c)
    private val check = ImageView(c)
    private val chevron = ImageView(c)

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPaddingRelative(Ui.dp(c, 16), Ui.dp(c, 8), Ui.dp(c, 14), Ui.dp(c, 8))
        background = Ui.rippleBg(c)
        isClickable = true
        minimumHeight = Ui.dp(c, 60)

        thumb.clipToOutline = true
        addView(thumb, LayoutParams(Ui.dp(c, 44), Ui.dp(c, 44)))

        val col = LinearLayout(c)
        col.orientation = VERTICAL
        title.textSize = 16f
        title.setTextColor(Ui.text(c))
        title.isSingleLine = true
        title.ellipsize = android.text.TextUtils.TruncateAt.MIDDLE
        title.textAlignment = TEXT_ALIGNMENT_VIEW_START
        sub.textSize = 13f
        sub.setTextColor(Ui.text2(c))
        sub.isSingleLine = true
        sub.textAlignment = TEXT_ALIGNMENT_VIEW_START
        col.addView(title, LayoutParams(-1, -2))
        col.addView(sub, LayoutParams(-1, -2))
        val cp = LayoutParams(0, -2, 1f)
        cp.marginStart = Ui.dp(c, 12)
        cp.marginEnd = Ui.dp(c, 8)
        addView(col, cp)

        check.setPadding(Ui.dp(c, 3), Ui.dp(c, 3), Ui.dp(c, 3), Ui.dp(c, 3))
        addView(check, LayoutParams(Ui.dp(c, 24), Ui.dp(c, 24)))

        chevron.setImageResource(R.drawable.ic_chevron)
        chevron.imageTintList = ColorStateList.valueOf(Ui.hint(c))
        addView(chevron, LayoutParams(Ui.dp(c, 18), Ui.dp(c, 18)))
        press(c)
    }

    private fun press(c: Context) {
        Ui.press(c, this)
    }

    fun bind(m: Item, px: Int) {
        val c = context
        val g = GradientDrawable()
        g.cornerRadius = Ui.dp(c, 8).toFloat()
        if (Thumbs.has(m)) {
            thumb.setPadding(0, 0, 0, 0)
            thumb.imageTintList = null
            thumb.scaleType = ImageView.ScaleType.CENTER_CROP
            g.setColor(Ui.high(c))
            thumb.background = g
            Thumbs.load(thumb, m, px)
        } else {
            Thumbs.clear(thumb)
            val p = Ui.dp(c, 10)
            thumb.setPadding(p, p, p, p)
            thumb.scaleType = ImageView.ScaleType.FIT_CENTER
            g.setColor(0xFF000000.toInt() or (m.kind.tint() and 0x00FFFFFF))
            thumb.background = g
            thumb.setImageResource(m.kind.icon())
            thumb.imageTintList = ColorStateList.valueOf(0xFFFFFFFF.toInt())
        }
        title.text = m.name
        sub.text = if (m.kind == Kind.FOLDER) {
            c.getString(R.string.folder) + "  \u00B7  " + Fmt.date(m.modified)
        } else {
            Fmt.size(m.size) + "  \u00B7  " + Fmt.date(m.modified)
        }
        chevron.visibility = if (m.kind == Kind.FOLDER) VISIBLE else GONE
        contentDescription = m.name
    }

    fun setSelection(selecting: Boolean, selected: Boolean, folder: Boolean) {
        val c = context
        if (!selecting || folder) {
            check.visibility = GONE
        } else {
            check.visibility = VISIBLE
            if (selected) {
                check.background = Ui.circle(c, Ui.accent(c), 0xFFFFFFFF.toInt(), 2)
                check.setImageResource(R.drawable.ic_check)
                check.imageTintList = ColorStateList.valueOf(0xFFFFFFFF.toInt())
            } else {
                check.background = Ui.circle(c, 0x00000000, Ui.stroke(c), 2)
                check.setImageDrawable(null)
            }
        }
        isSelected = selected
    }
}
