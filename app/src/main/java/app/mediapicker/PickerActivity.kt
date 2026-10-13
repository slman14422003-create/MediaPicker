package app.mediapicker

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback

/**
 * Answers "choose a file" requests (GET_CONTENT, OPEN_DOCUMENT, PICK, PICK_IMAGES) from other apps with the
 * browser over every photo, video, audio file and document on the device.
 */
class PickerActivity : BaseActivity(), BrowserView.Host {
    private var multiple = false
    private var max = Int.MAX_VALUE
    private var types: Array<String> = arrayOf("*/*")
    private var openDoc = false
    private var showFiles = true
    private lateinit var browser: BrowserView
    private lateinit var titleView: TextView
    private lateinit var done: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        readRequest(intent)

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Ui.bg(this))
        setRoot(root)

        val head = LinearLayout(this)
        head.gravity = Gravity.CENTER_VERTICAL
        head.setPadding(Ui.dp(this, 8), Ui.dp(this, 8), Ui.dp(this, 14), Ui.dp(this, 6))
        head.addView(
            Ui.iconButton(this, R.drawable.ic_cancel, getString(R.string.cancel)) { cancel() },
            LinearLayout.LayoutParams(Ui.dp(this, 44), Ui.dp(this, 44))
        )
        titleView = TextView(this)
        titleView.setTextColor(Ui.text(this))
        titleView.textSize = 17f
        titleView.typeface = Ui.medium()
        titleView.setPadding(Ui.dp(this, 6), 0, Ui.dp(this, 6), 0)
        head.addView(titleView, LinearLayout.LayoutParams(0, -2, 1f))
        head.addView(
            Ui.iconButton(this, R.drawable.ic_sort, getString(R.string.sort_by)) { browser.showSort() },
            LinearLayout.LayoutParams(Ui.dp(this, 44), Ui.dp(this, 44))
        )
        done = TextView(this)
        done.setText(R.string.done)
        done.setTextColor(Ui.res(this, R.color.on_accent))
        done.textSize = 14.5f
        done.setTypeface(null, android.graphics.Typeface.BOLD)
        done.setBackgroundResource(R.drawable.btn_primary)
        done.gravity = Gravity.CENTER
        done.setPadding(Ui.dp(this, 18), 0, Ui.dp(this, 18), 0)
        done.setOnClickListener { finishMultiple() }
        done.visibility = if (multiple) View.VISIBLE else View.GONE
        val dp = LinearLayout.LayoutParams(-2, Ui.dp(this, 42))
        dp.marginStart = Ui.dp(this, 8)
        head.addView(done, dp)
        root.addView(head)

        browser = BrowserView(this, this)
        root.addView(browser, LinearLayout.LayoutParams(-1, 0, 1f))
        updateTitle(0)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!browser.handleBack()) cancel()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        browser.onResume()
    }

    override fun onAccessChanged() {
        browser.onResume()
    }

    override fun onSystemPicked(uris: List<Uri>) {
        deliver(if (multiple) uris.take(max) else uris.take(1))
    }

    // ------------------------------------------------------------------ request

    private fun readRequest(i: Intent?) {
        if (i == null) return
        val list = ArrayList<String>()
        val extra = i.getStringArrayExtra(Intent.EXTRA_MIME_TYPES)
        if (extra != null) list.addAll(extra)
        if (list.isEmpty()) {
            val t = i.type
            if (t != null) list.add(t)
        }
        val mapped = ArrayList<String>()
        for (t in list) {
            mapped.add(
                when {
                    t.startsWith("vnd.android.cursor") && t.endsWith("/image") -> "image/*"
                    t.startsWith("vnd.android.cursor") && t.endsWith("/video") -> "video/*"
                    t.startsWith("vnd.android.cursor") && t.endsWith("/audio") -> "audio/*"
                    else -> t
                }
            )
        }
        val photoPicker = i.action == "android.provider.action.PICK_IMAGES"
        if (mapped.isEmpty() && photoPicker) {
            mapped.add("image/*")
            mapped.add("video/*")
        }
        types = if (mapped.isEmpty()) arrayOf("*/*") else mapped.toTypedArray()
        multiple = i.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
        if (photoPicker) {
            showFiles = false
            val m = i.getIntExtra("android.provider.extra.PICK_IMAGES_MAX", 1)
            if (m > 1) {
                multiple = true
                max = m
            }
        }
        openDoc = i.action == Intent.ACTION_OPEN_DOCUMENT
    }

    private fun mimeMatches(mime: String): Boolean {
        for (t in types) {
            if (t == "*/*") return true
            if (t.endsWith("/*")) {
                if (mime.startsWith(t.substring(0, t.length - 1))) return true
            } else if (t.equals(mime, ignoreCase = true)) {
                return true
            }
        }
        return false
    }

    override fun pickMode(): Boolean = true
    override fun multiple(): Boolean = multiple
    override fun max(): Int = max
    override fun filesTab(): Boolean = showFiles

    override fun allows(kind: Kind): Boolean {
        val prefix = when (kind) {
            Kind.IMAGE -> "image/"
            Kind.VIDEO -> "video/"
            Kind.AUDIO -> "audio/"
            else -> return true
        }
        return types.any { it == "*/*" || it.startsWith(prefix) }
    }

    override fun accepts(item: Item): Boolean = item.kind == Kind.FOLDER || mimeMatches(item.mime)

    override fun selectionChanged(count: Int) {
        updateTitle(count)
    }

    private fun updateTitle(count: Int) {
        titleView.text = if (multiple && count > 0) getString(R.string.pick_selected, count) else getString(R.string.pick_title)
    }

    override fun open(item: Item) {
        pick(listOf(item))
    }

    override fun askMedia() {
        requestMedia()
    }

    override fun askAllFiles() {
        requestAllFiles()
    }

    override fun browseSystem() {
        browseSystem(multiple, types)
    }

    // ------------------------------------------------------------------ result

    private fun cancel() {
        setResult(RESULT_CANCELED)
        finish()
    }

    private fun finishMultiple() {
        pick(browser.selected())
    }

    override fun pick(items: List<Item>) {
        if (items.isEmpty()) {
            cancel()
            return
        }
        deliver(items.map { it.shareUri(this) })
    }

    private fun deliver(uris: List<Uri>) {
        if (uris.isEmpty()) {
            cancel()
            return
        }
        val out = Intent()
        val clip = ClipData.newRawUri("media", uris[0])
        for (k in 1 until uris.size) clip.addItem(ClipData.Item(uris[k]))
        out.data = uris[0]
        out.clipData = clip
        var flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        if (openDoc) flags = flags or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
        out.addFlags(flags)
        setResult(RESULT_OK, out)
        finish()
    }
}
