package app.mediapicker

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

/** Launcher screen: browse every photo, video, audio file, folder and document, and open each in its linked app. */
class MainActivity : BaseActivity(), BrowserView.Host {
    private lateinit var browser: BrowserView
    private lateinit var titleView: TextView
    private lateinit var btnSettings: ImageButton
    private lateinit var btnSort: ImageButton
    private lateinit var btnCancel: ImageButton
    private lateinit var btnShare: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Ui.bg(this))
        setRoot(root)

        val head = LinearLayout(this)
        head.gravity = Gravity.CENTER_VERTICAL
        head.minimumHeight = Ui.dp(this, 64)
        head.setPadding(Ui.dp(this, 16), Ui.dp(this, 8), Ui.dp(this, 12), Ui.dp(this, 6))

        btnCancel = Ui.iconButton(this, R.drawable.ic_cancel, getString(R.string.cancel)) { browser.clearSelection() }
        btnCancel.visibility = View.GONE
        val cp = LinearLayout.LayoutParams(Ui.dp(this, 40), Ui.dp(this, 40))
        cp.marginEnd = Ui.dp(this, 8)
        head.addView(btnCancel, cp)

        titleView = TextView(this)
        titleView.text = getString(R.string.app_name)
        titleView.setTextColor(Ui.text(this))
        titleView.textSize = 30f
        titleView.setTypeface(null, android.graphics.Typeface.BOLD)
        titleView.maxLines = 1
        titleView.ellipsize = android.text.TextUtils.TruncateAt.END
        head.addView(titleView, LinearLayout.LayoutParams(0, -2, 1f))

        btnShare = Ui.iconButton(this, R.drawable.ic_share, getString(R.string.share)) {
            Opener.share(this, browser.selected())
        }
        btnShare.visibility = View.GONE
        val sp = LinearLayout.LayoutParams(Ui.dp(this, 40), Ui.dp(this, 40))
        sp.marginStart = Ui.dp(this, 8)
        head.addView(btnShare, sp)

        btnSort = Ui.iconButton(this, R.drawable.ic_sort, getString(R.string.sort_by)) { browser.showSort() }
        val so = LinearLayout.LayoutParams(Ui.dp(this, 40), Ui.dp(this, 40))
        so.marginStart = Ui.dp(this, 8)
        head.addView(btnSort, so)

        btnSettings = Ui.iconButton(this, R.drawable.ic_settings, getString(R.string.settings)) {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        val st = LinearLayout.LayoutParams(Ui.dp(this, 40), Ui.dp(this, 40))
        st.marginStart = Ui.dp(this, 8)
        head.addView(btnSettings, st)
        root.addView(head)

        browser = BrowserView(this, this)
        root.addView(browser, LinearLayout.LayoutParams(-1, 0, 1f))

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!browser.handleBack()) {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
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
        val u = uris[0]
        Opener.openUri(this, u, contentResolver.getType(u), null)
    }

    override fun pickMode(): Boolean = false
    override fun multiple(): Boolean = true
    override fun max(): Int = Int.MAX_VALUE
    override fun filesTab(): Boolean = true
    override fun allows(kind: Kind): Boolean = true
    override fun accepts(item: Item): Boolean = true

    override fun selectionChanged(count: Int) {
        val on = count > 0
        titleView.text = if (on) getString(R.string.pick_selected, count) else getString(R.string.app_name)
        titleView.textSize = if (on) 20f else 30f
        btnCancel.visibility = if (on) View.VISIBLE else View.GONE
        btnShare.visibility = if (on) View.VISIBLE else View.GONE
        btnSort.visibility = if (on) View.GONE else View.VISIBLE
        btnSettings.visibility = if (on) View.GONE else View.VISIBLE
    }

    override fun pick(items: List<Item>) {
        if (items.isNotEmpty()) Opener.open(this, items[0])
    }

    override fun open(item: Item) {
        Opener.open(this, item)
    }

    override fun askMedia() {
        requestMedia()
    }

    override fun askAllFiles() {
        requestAllFiles()
    }

    override fun browseSystem() {
        browseSystem(false, null)
    }
}
