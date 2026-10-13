package app.mediapicker

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

/** One square cell of the grid: thumbnail (or type icon + name), duration badge and selection ring. */
class Tile(c: Context) : FrameLayout(c) {
    val thumb = ImageView(c)
    private val icon = ImageView(c)
    private val label = TextView(c)
    private val badge = LinearLayout(c)
    private val dur = TextView(c)
    private val scrim = View(c)
    private val check = ImageView(c)

    init {
        setBackgroundColor(Ui.high(c))
        thumb.scaleType = ImageView.ScaleType.CENTER_CROP
        addView(thumb, LayoutParams(-1, -1))

        icon.scaleType = ImageView.ScaleType.FIT_CENTER
        icon.imageTintList = ColorStateList.valueOf(Ui.text2(c))
        val ip = LayoutParams(Ui.dp(c, 34), Ui.dp(c, 34), Gravity.CENTER)
        ip.bottomMargin = Ui.dp(c, 12)
        addView(icon, ip)

        label.textSize = 11f
        label.maxLines = 2
        label.gravity = Gravity.CENTER
        label.setTextColor(Ui.text(c))
        label.setPadding(Ui.dp(c, 6), 0, Ui.dp(c, 6), Ui.dp(c, 6))
        addView(label, LayoutParams(-1, -2, Gravity.BOTTOM))

        scrim.setBackgroundColor(0x33000000)
        scrim.visibility = GONE
        addView(scrim, LayoutParams(-1, -1))

        badge.orientation = LinearLayout.HORIZONTAL
        badge.gravity = Gravity.CENTER_VERTICAL
        val g = GradientDrawable()
        g.cornerRadius = Ui.dp(c, 20).toFloat()
        g.setColor(0x99000000.toInt())
        badge.background = g
        badge.setPadding(Ui.dp(c, 6), Ui.dp(c, 2), Ui.dp(c, 7), Ui.dp(c, 2))
        val pi = ImageView(c)
        pi.setImageResource(R.drawable.ic_play_fill)
        pi.imageTintList = ColorStateList.valueOf(0xFFFFFFFF.toInt())
        badge.addView(pi, LinearLayout.LayoutParams(Ui.dp(c, 13), Ui.dp(c, 13)))
        dur.setTextColor(0xFFFFFFFF.toInt())
        dur.textSize = 11f
        dur.typeface = Typeface.DEFAULT_BOLD
        dur.setPadding(Ui.dp(c, 3), 0, 0, 0)
        badge.addView(dur)
        val bp = LayoutParams(-2, -2, Gravity.TOP or Gravity.END)
        bp.setMargins(Ui.dp(c, 6), Ui.dp(c, 6), Ui.dp(c, 6), Ui.dp(c, 6))
        addView(badge, bp)

        val p = Ui.dp(c, 3)
        check.setPadding(p, p, p, p)
        val cp = LayoutParams(Ui.dp(c, 24), Ui.dp(c, 24), Gravity.BOTTOM or Gravity.END)
        cp.setMargins(Ui.dp(c, 6), Ui.dp(c, 6), Ui.dp(c, 6), Ui.dp(c, 6))
        addView(check, cp)
        isClickable = true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec)
    }

    fun bind(m: Item, px: Int) {
        val hasThumb = Thumbs.has(m)
        if (hasThumb) {
            thumb.visibility = VISIBLE
            icon.visibility = GONE
            label.visibility = GONE
            Thumbs.load(thumb, m, px)
        } else {
            Thumbs.clear(thumb)
            thumb.setImageDrawable(null)
            thumb.visibility = GONE
            icon.setImageResource(m.kind.icon())
            icon.visibility = VISIBLE
            label.text = m.name
            label.visibility = VISIBLE
        }
        val timed = m.kind == Kind.VIDEO && m.duration > 0
        dur.text = if (timed) Fmt.dur(m.duration) else ""
        badge.visibility = if (timed) VISIBLE else GONE
        contentDescription = m.name
    }

    /** Selecting shows a ring in the corner; a selected cell gets a filled blue circle with a check. */
    fun setSelection(selecting: Boolean, selected: Boolean) {
        val c = context
        scrim.visibility = if (selected) VISIBLE else GONE
        if (!selecting) {
            check.visibility = GONE
        } else {
            check.visibility = VISIBLE
            if (selected) {
                check.background = Ui.circle(c, Ui.accent(c), 0xFFFFFFFF.toInt(), 2)
                check.setImageResource(R.drawable.ic_check)
                check.imageTintList = ColorStateList.valueOf(0xFFFFFFFF.toInt())
            } else {
                check.background = Ui.circle(c, 0x33000000, 0xFFFFFFFF.toInt(), 2)
                check.setImageDrawable(null)
            }
        }
        isSelected = selected
    }
}
