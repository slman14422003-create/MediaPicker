package app.mediapicker

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import java.io.File

object Repo {
    @Volatile
    var media: List<Item> = emptyList()

    @Volatile
    var loaded = false

    /** Reads every photo, video and audio file known to MediaStore (call from a background thread). */
    fun loadMedia(c: Context): List<Item> {
        val out = ArrayList<Item>()
        query(c, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, Kind.IMAGE, out)
        query(c, MediaStore.Video.Media.EXTERNAL_CONTENT_URI, Kind.VIDEO, out)
        query(c, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, Kind.AUDIO, out)
        out.sortByDescending { it.modified }
        media = out
        loaded = true
        return out
    }

    private fun query(c: Context, base: Uri, kind: Kind, out: MutableList<Item>) {
        val hasBucket = kind != Kind.AUDIO || Build.VERSION.SDK_INT >= 29
        val cols = ArrayList<String>()
        cols.add("_id")
        cols.add("_display_name")
        cols.add("mime_type")
        cols.add("_size")
        cols.add("date_modified")
        if (kind != Kind.IMAGE) cols.add("duration")
        if (hasBucket) cols.add("bucket_display_name")
        try {
            val cur = c.contentResolver.query(base, cols.toTypedArray(), null, null, "date_modified DESC") ?: return
            cur.use {
                val iId = it.getColumnIndex("_id")
                val iName = it.getColumnIndex("_display_name")
                val iMime = it.getColumnIndex("mime_type")
                val iSize = it.getColumnIndex("_size")
                val iDate = it.getColumnIndex("date_modified")
                val iDur = it.getColumnIndex("duration")
                val iBucket = it.getColumnIndex("bucket_display_name")
                while (it.moveToNext()) {
                    val id = it.getLong(iId)
                    val name = (if (iName >= 0) it.getString(iName) else null) ?: "file"
                    val rawMime: String? = if (iMime >= 0) it.getString(iMime) else null
                    val mime: String = if (rawMime.isNullOrEmpty()) Mimes.mimeOf(name) else rawMime
                    val bucket = (if (iBucket >= 0) it.getString(iBucket) else null) ?: ""
                    out.add(
                        Item(
                            kind, name, mime,
                            if (iSize >= 0) it.getLong(iSize) else 0L,
                            if (iDate >= 0) it.getLong(iDate) else 0L,
                            bucket,
                            if (iDur >= 0) it.getLong(iDur) else 0L,
                            ContentUris.withAppendedId(base, id), null
                        )
                    )
                }
            }
        } catch (e: Exception) {
        }
    }

    fun listDir(dir: File, hidden: Boolean): List<Item> {
        val arr = dir.listFiles() ?: return emptyList()
        val out = ArrayList<Item>(arr.size)
        for (f in arr) {
            if (!hidden && f.name.startsWith(".")) continue
            if (f.isDirectory) {
                out.add(Item(Kind.FOLDER, f.name, "inode/directory", 0L, f.lastModified() / 1000, "", 0L, null, f))
            } else {
                val m = Mimes.mimeOf(f.name)
                out.add(Item(Mimes.kindOf(m, f.name), f.name, m, f.length(), f.lastModified() / 1000, "", 0L, null, f))
            }
        }
        return out
    }
}
