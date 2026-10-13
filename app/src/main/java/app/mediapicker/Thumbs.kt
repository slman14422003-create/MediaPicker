package app.mediapicker

import android.net.Uri
import android.widget.ImageView
import com.bumptech.glide.Glide

object Thumbs {
    private fun source(i: Item): Uri? = when {
        i.kind != Kind.IMAGE && i.kind != Kind.VIDEO -> null
        i.uri != null -> i.uri
        i.file != null -> Uri.fromFile(i.file)
        else -> null
    }

    fun has(i: Item): Boolean = source(i) != null

    fun load(v: ImageView, i: Item, px: Int) {
        val s = source(i)
        if (s == null) {
            Glide.with(v).clear(v)
            v.setImageDrawable(null)
            return
        }
        Glide.with(v).load(s).override(px).centerCrop().into(v)
    }

    fun clear(v: ImageView) {
        Glide.with(v).clear(v)
    }
}
