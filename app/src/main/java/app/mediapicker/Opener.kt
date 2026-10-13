package app.mediapicker

import android.app.Activity
import android.content.ClipData
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Parcelable
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

/** Opens, shares and forwards files to the apps the user linked to each file type. */
object Opener {
    private fun exclusions(c: Context): Array<Parcelable> =
        arrayOf<Parcelable>(ComponentName(c, OpenActivity::class.java))

    fun open(c: Context, item: Item) {
        openUri(c, item.shareUri(c), item.mime, item.name)
    }

    fun openUri(c: Context, uri: Uri, typeIn: String?, nameHint: String?) {
        val name = nameHint ?: (uri.lastPathSegment ?: "")
        var mime: String = typeIn ?: ""
        if (mime.isEmpty() || mime == "*/*") mime = c.contentResolver.getType(uri) ?: ""
        if (mime.isEmpty() || mime == "*/*") mime = Mimes.mimeOf(name)
        val cat = Mimes.category(mime, name)
        val view = Intent(Intent.ACTION_VIEW)
        view.setDataAndType(uri, mime)
        view.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        view.clipData = ClipData.newRawUri("", uri)
        if (c !is Activity) view.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            val pkg = Store.linked(c, cat)
            if (pkg != null) {
                val direct = Intent(view)
                direct.setPackage(pkg)
                if (direct.resolveActivity(c.packageManager) != null) {
                    c.startActivity(direct)
                    return
                }
            }
            val ch = Intent.createChooser(view, c.getString(R.string.open_with))
            ch.putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS, exclusions(c))
            ch.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (c !is Activity) ch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            c.startActivity(ch)
        } catch (e: Exception) {
            Toast.makeText(c, R.string.no_app, Toast.LENGTH_LONG).show()
        }
    }

    /** Called by the exported "open with" screen: hands a file from another app to the linked app. */
    fun forward(a: Activity, data: Uri, type: String?) {
        var uri = data
        if (uri.scheme == "file") {
            try {
                uri = FileProvider.getUriForFile(a, a.packageName + ".files", File(uri.path ?: ""))
            } catch (e: Exception) {
                Toast.makeText(a, R.string.no_app, Toast.LENGTH_LONG).show()
                return
            }
        }
        openUri(a, uri, type, null)
    }

    fun share(c: Context, items: List<Item>) {
        if (items.isEmpty()) return
        val uris = ArrayList<Uri>()
        for (i in items) uris.add(i.shareUri(c))
        val send = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, uris[0])
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
        }
        send.type = commonMime(items)
        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val clip = ClipData.newRawUri("", uris[0])
        for (k in 1 until uris.size) clip.addItem(ClipData.Item(uris[k]))
        send.clipData = clip
        try {
            val ch = Intent.createChooser(send, c.getString(R.string.share))
            if (c !is Activity) ch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            c.startActivity(ch)
        } catch (e: Exception) {
            Toast.makeText(c, R.string.no_app, Toast.LENGTH_LONG).show()
        }
    }

    private fun commonMime(items: List<Item>): String {
        val first = items[0].mime
        if (items.all { it.mime == first }) return first
        val major = first.substringBefore('/')
        return if (items.all { it.mime.substringBefore('/') == major }) "$major/*" else "*/*"
    }

    /** Apps (other than this one) that can open the sample type of a category. */
    fun candidates(c: Context, cat: Cat): List<ResolveInfo> {
        val i = Intent(Intent.ACTION_VIEW)
        i.setDataAndType(Uri.parse("content://" + c.packageName + ".files/sample"), cat.sample)
        val list = c.packageManager.queryIntentActivities(i, PackageManager.MATCH_DEFAULT_ONLY)
        return list.filter { it.activityInfo.packageName != c.packageName }
            .distinctBy { it.activityInfo.packageName }
            .sortedBy { it.loadLabel(c.packageManager).toString().lowercase() }
    }
}
