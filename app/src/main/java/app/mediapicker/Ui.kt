package app.mediapicker

import android.animation.AnimatorInflater
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.RippleDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat

class Row(
    val icon: Int,
    val title: String,
    val sub: String?,
    val tint: Int,
    val chevron: Boolean = true,
    val badge: String? = null,
    val badgeColor: Int = 0
)

class Toggle(val view: View, private val sw: SwitchCompat) {
    fun isChecked(): Boolean = sw.isChecked

    fun onChange(l: (Boolean) -> Unit) {
        sw.setOnCheckedChangeListener { _, on -> l(on) }
    }
}

/** Shared look: iOS system colours, grouped rows, capsule chips, round icon buttons. */
object Ui {
    fun dp(c: Context, v: Int): Int = (v * c.resources.displayMetrics.density).toInt()

    private fun attr(c: Context, a: Int): Int {
        val tv = TypedValue()
        return if (c.theme.resolveAttribute(a, tv, true) &&
            tv.type >= TypedValue.TYPE_FIRST_COLOR_INT && tv.type <= TypedValue.TYPE_LAST_COLOR_INT
        ) tv.data else 0
    }

    fun bg(c: Context): Int = attr(c, R.attr.cBg)
    fun group(c: Context): Int = attr(c, R.attr.cGroupBg)
    fun surface(c: Context): Int = attr(c, R.attr.cSurface)
    fun high(c: Context): Int = attr(c, R.attr.cSurfaceHigh)
    fun stroke(c: Context): Int = attr(c, R.attr.cStroke)
    fun text(c: Context): Int = attr(c, R.attr.cTextPrimary)
    fun text2(c: Context): Int = attr(c, R.attr.cTextSecondary)
    fun hint(c: Context): Int = attr(c, R.attr.cTextHint)
    fun accent(c: Context): Int = attr(c, R.attr.cAccent)
    fun neutral(c: Context): Int = attr(c, R.attr.cNeutralSoft)
    fun res(c: Context, id: Int): Int = ContextCompat.getColor(c, id)

    fun press(c: Context, v: View?) {
        if (v == null || v.stateListAnimator != null) return
        v.stateListAnimator = AnimatorInflater.loadStateListAnimator(c, R.animator.press_scale)
    }

    fun medium(): Typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)

    fun iconButton(c: Context, icon: Int, desc: String, onClick: () -> Unit): ImageButton {
        val b = ImageButton(c, null, 0, R.style.IconButton)
        b.setImageResource(icon)
        b.contentDescription = desc
        b.setOnClickListener { onClick() }
        return b
    }

    fun chip(c: Context, text: CharSequence, selected: Boolean): TextView {
        val t = TextView(c)
        t.text = text
        t.textSize = 14f
        t.isSingleLine = true
        t.typeface = medium()
        t.setPadding(dp(c, 16), dp(c, 8), dp(c, 16), dp(c, 8))
        val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.marginEnd = dp(c, 8)
        t.layoutParams = lp
        setChip(c, t, selected)
        press(c, t)
        return t
    }

    fun setChip(c: Context, t: TextView, selected: Boolean) {
        val g = GradientDrawable()
        g.cornerRadius = dp(c, 100).toFloat()
        g.setColor(if (selected) accent(c) else neutral(c))
        t.background = g
        t.setTextColor(if (selected) res(c, R.color.on_accent) else text(c))
    }

    fun button(c: Context, label: CharSequence, primary: Boolean): Button {
        val b = Button(c)
        b.text = label
        b.isAllCaps = false
        b.setBackgroundResource(if (primary) R.drawable.btn_primary else R.drawable.btn_secondary)
        b.setTextColor(if (primary) res(c, R.color.on_accent) else text(c))
        b.textSize = 16f
        b.typeface = medium()
        b.minHeight = dp(c, 50)
        b.minimumHeight = dp(c, 50)
        press(c, b)
        return b
    }

    fun rippleBg(c: Context): Drawable =
        RippleDrawable(ColorStateList.valueOf(res(c, R.color.ripple)), null, ColorDrawable(0xFFFFFFFF.toInt()))

    fun circle(c: Context, fill: Int, strokeColor: Int, strokeDp: Int): GradientDrawable {
        val g = GradientDrawable()
        g.shape = GradientDrawable.OVAL
        g.setColor(fill)
        if (strokeDp > 0) g.setStroke(dp(c, strokeDp), strokeColor)
        return g
    }

    /** Pick-one dialog: current choice marked with a filled dot, like the iOS style lists. */
    fun choose(c: Context, title: Int, names: List<String>, current: Int, onPick: (Int) -> Unit) {
        val marked = names.mapIndexed { i, n -> (if (i == current) "\u25CF " else "\u25CB ") + n }
        AlertDialog.Builder(c)
            .setTitle(title)
            .setItems(marked.toTypedArray()) { d, which ->
                d.dismiss()
                onPick(which)
            }
            .show()
    }

    // ------------------------------------------------------------------ settings building blocks

    fun sectionTitle(c: Context, text: CharSequence): TextView {
        val t = TextView(c)
        t.text = text
        t.setTextColor(text2(c))
        t.textSize = 13f
        t.textAlignment = View.TEXT_ALIGNMENT_VIEW_START
        t.setPaddingRelative(dp(c, 32), dp(c, 24), dp(c, 32), dp(c, 7))
        return t
    }

    fun footer(c: Context, text: CharSequence): TextView {
        val t = TextView(c)
        t.text = text
        t.setTextColor(text2(c))
        t.textSize = 13f
        t.setLineSpacing(0f, 1.15f)
        t.textAlignment = View.TEXT_ALIGNMENT_VIEW_START
        t.setPaddingRelative(dp(c, 32), dp(c, 8), dp(c, 32), dp(c, 4))
        return t
    }

    fun hero(c: Context, name: CharSequence, sub: CharSequence): View {
        val box = LinearLayout(c)
        box.orientation = LinearLayout.VERTICAL
        box.gravity = Gravity.CENTER_HORIZONTAL
        box.setPadding(dp(c, 16), dp(c, 12), dp(c, 16), dp(c, 6))
        val icon = ImageView(c)
        icon.setImageResource(R.drawable.ic_brand_mark)
        icon.scaleType = ImageView.ScaleType.FIT_CENTER
        val g = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(0xFFFFFFFF.toInt(), 0xFFEAF3FF.toInt()))
        g.cornerRadius = dp(c, 20).toFloat()
        g.setStroke(1, 0x14000000)
        icon.background = g
        icon.clipToOutline = true
        box.addView(icon, LinearLayout.LayoutParams(dp(c, 84), dp(c, 84)))
        val n = TextView(c)
        n.text = name
        n.setTextColor(text(c))
        n.textSize = 22f
        n.typeface = medium()
        n.gravity = Gravity.CENTER
        val np = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        np.topMargin = dp(c, 12)
        box.addView(n, np)
        val v = TextView(c)
        v.text = sub
        v.setTextColor(text2(c))
        v.textSize = 13f
        v.gravity = Gravity.CENTER
        box.addView(v, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        return box
    }

    private fun bindIcon(c: Context, icon: ImageView, res: Int, tint: Int) {
        icon.setImageResource(res)
        icon.imageTintList = ColorStateList.valueOf(0xFFFFFFFF.toInt())
        val g = GradientDrawable()
        g.cornerRadius = dp(c, 8).toFloat()
        g.setColor(0xFF000000.toInt() or (tint and 0x00FFFFFF))
        icon.background = g
        icon.visibility = View.VISIBLE
    }

    fun rowView(c: Context, parent: ViewGroup, row: Row, click: (() -> Unit)?): View {
        val v = LayoutInflater.from(c).inflate(R.layout.item_row, parent, false)
        press(c, v.findViewById(R.id.card))
        bindIcon(c, v.findViewById(R.id.icon), row.icon, row.tint)
        v.findViewById<TextView>(R.id.title).text = row.title
        val sub = v.findViewById<TextView>(R.id.sub)
        if (row.sub.isNullOrEmpty()) {
            sub.visibility = View.GONE
        } else {
            sub.text = row.sub
            sub.visibility = View.VISIBLE
        }
        val badge = v.findViewById<TextView>(R.id.badge)
        if (row.badge.isNullOrEmpty()) {
            badge.visibility = View.GONE
        } else {
            badge.text = row.badge
            badge.setTextColor(row.badgeColor)
            val g = GradientDrawable()
            g.cornerRadius = dp(c, 100).toFloat()
            g.setColor((row.badgeColor and 0x00FFFFFF) or 0x26000000)
            badge.background = g
            badge.visibility = View.VISIBLE
        }
        v.findViewById<View>(R.id.lock).visibility = View.GONE
        v.findViewById<View>(R.id.chevron).visibility = if (row.chevron) View.VISIBLE else View.INVISIBLE
        if (click != null) v.setOnClickListener { click() }
        return v
    }

    fun toggle(c: Context, parent: ViewGroup, title: Int, sub: Int, checked: Boolean, icon: Int, tint: Int): Toggle {
        val v = LayoutInflater.from(c).inflate(R.layout.item_toggle, parent, false)
        v.findViewById<TextView>(R.id.title).setText(title)
        val s = v.findViewById<TextView>(R.id.sub)
        if (sub != 0) {
            s.setText(sub)
            s.visibility = View.VISIBLE
        }
        bindIcon(c, v.findViewById(R.id.icon), icon, tint)
        val sw = v.findViewById<SwitchCompat>(R.id.sw)
        sw.thumbDrawable = ContextCompat.getDrawable(c, R.drawable.switch_thumb)
        sw.trackDrawable = ContextCompat.getDrawable(c, R.drawable.switch_track)
        sw.showText = false
        sw.isChecked = checked
        v.setOnClickListener { sw.toggle() }
        return Toggle(v, sw)
    }

    /** Shapes a row card so rows of one section read as a single rounded group. */
    private fun shapeRow(c: Context, v: View, first: Boolean, last: Boolean) {
        val card = v.findViewById<View>(R.id.card) ?: return
        val big = dp(c, 12).toFloat()
        val top = if (first) big else 0f
        val bottom = if (last) big else 0f
        val radii = floatArrayOf(top, top, top, top, bottom, bottom, bottom, bottom)
        val fill = GradientDrawable()
        fill.setColor(surface(c))
        fill.cornerRadii = radii
        val mask = GradientDrawable()
        mask.setColor(0xFFFFFFFF.toInt())
        mask.cornerRadii = radii
        var base: Drawable = fill
        if (!last) {
            val line = GradientDrawable()
            line.setColor(stroke(c))
            val ld = LayerDrawable(arrayOf<Drawable>(fill, line))
            ld.setLayerGravity(1, Gravity.BOTTOM or Gravity.FILL_HORIZONTAL)
            ld.setLayerHeight(1, Math.max(1, Math.round(c.resources.displayMetrics.density * 0.5f)))
            val ic = card.findViewById<View>(R.id.icon)
            ld.setLayerInsetStart(1, dp(c, if (ic != null && ic.visibility == View.VISIBLE) 58 else 16))
            base = ld
        }
        card.background = RippleDrawable(ColorStateList.valueOf(res(c, R.color.ripple)), base, mask)
    }

    private fun groupRows(c: Context, g: ViewGroup) {
        val n = g.childCount
        for (i in 0 until n) {
            val v = g.getChildAt(i)
            if (v.findViewById<View>(R.id.card) == null) continue
            val first = i == 0 || g.getChildAt(i - 1).findViewById<View>(R.id.card) == null
            val last = i == n - 1 || g.getChildAt(i + 1).findViewById<View>(R.id.card) == null
            shapeRow(c, v, first, last)
        }
    }

    /** Keeps row groups shaped automatically while a screen adds or removes rows. */
    fun autoGroup(c: Context, g: ViewGroup) {
        val r = Runnable { groupRows(c, g) }
        g.setOnHierarchyChangeListener(object : ViewGroup.OnHierarchyChangeListener {
            override fun onChildViewAdded(parent: View?, child: View?) {
                g.removeCallbacks(r)
                g.post(r)
            }

            override fun onChildViewRemoved(parent: View?, child: View?) {
                g.removeCallbacks(r)
                g.post(r)
            }
        })
    }
}
