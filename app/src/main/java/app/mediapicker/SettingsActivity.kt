package app.mediapicker

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog

/** Appearance, default picker, linked apps per file type, browser options and permissions. */
class SettingsActivity : BaseActivity() {
    private lateinit var content: LinearLayout

    private val BLUE = 0xFF007AFF.toInt()
    private val INDIGO = 0xFF5856D6.toInt()
    private val PURPLE = 0xFFAF52DE.toInt()
    private val GREEN = 0xFF34C759.toInt()
    private val ORANGE = 0xFFFF9500.toInt()
    private val RED = 0xFFFF3B30.toInt()
    private val GRAY = 0xFF8E8E93.toInt()
    private val TEAL = 0xFF30B0C7.toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawable(ColorDrawable(Ui.group(this)))

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Ui.group(this))
        setRoot(root)

        val head = LinearLayout(this)
        head.gravity = Gravity.CENTER_VERTICAL
        head.minimumHeight = Ui.dp(this, 64)
        head.setPadding(Ui.dp(this, 16), Ui.dp(this, 8), Ui.dp(this, 16), Ui.dp(this, 6))
        val back = Ui.iconButton(this, R.drawable.ic_back, getString(R.string.back)) { finish() }
        val bp = LinearLayout.LayoutParams(Ui.dp(this, 40), Ui.dp(this, 40))
        bp.marginEnd = Ui.dp(this, 8)
        head.addView(back, bp)
        val t = TextView(this)
        t.setText(R.string.settings)
        t.setTextColor(Ui.text(this))
        t.textSize = 30f
        t.setTypeface(null, android.graphics.Typeface.BOLD)
        head.addView(t, LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(head)

        val scroll = ScrollView(this)
        scroll.isFillViewport = true
        scroll.isVerticalScrollBarEnabled = false
        content = LinearLayout(this)
        content.orientation = LinearLayout.VERTICAL
        content.setPadding(0, 0, 0, Ui.dp(this, 28))
        scroll.addView(content, LinearLayout.LayoutParams(-1, -2))
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        Ui.autoGroup(this, content)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    override fun onAccessChanged() {
        render()
    }

    private fun filePickerIntent() = Defaults.filePicker()

    private fun photoPickerIntent() = Defaults.photoPicker()

    private fun isDefault(i: Intent) = Defaults.isDefault(this, i)

    private fun makeDefault(i: Intent) = Defaults.make(this, i)

    private fun openDefaultApps() = Defaults.openApps(this)

    private fun appLabel(cat: Cat, pkg: String?): String {
        if (pkg == null) return getString(R.string.ask_every_time)
        for (ri in Opener.candidates(this, cat)) {
            if (ri.activityInfo.packageName == pkg) return ri.loadLabel(packageManager).toString()
        }
        return pkg
    }

    private fun pickApp(cat: Cat) {
        val list = Opener.candidates(this, cat)
        val names = ArrayList<String>()
        names.add(getString(R.string.ask_every_time))
        for (ri in list) names.add(ri.loadLabel(packageManager).toString())
        val cur = Store.linked(this, cat.id)
        var idx = 0
        for (k in list.indices) if (list[k].activityInfo.packageName == cur) idx = k + 1
        Ui.choose(this, cat.title, names, idx) { which ->
            Store.setLinked(this, cat.id, if (which == 0) null else list[which - 1].activityInfo.packageName)
            render()
        }
    }

    private fun accessText(): Int = when {
        !Perms.hasMedia(this) -> R.string.access_denied
        Perms.isPartial(this) -> R.string.access_partial
        else -> R.string.access_granted
    }

    private fun render() {
        content.removeAllViews()
        val c = this

        content.addView(Ui.hero(c, getString(R.string.app_name), getString(R.string.set_version, BuildConfig.VERSION_NAME)))

        // appearance
        content.addView(Ui.sectionTitle(c, getString(R.string.theme_title)))
        content.addView(
            Ui.rowView(
                c, content,
                Row(R.drawable.ic_palette, getString(R.string.theme_title), getString(Appearance.name(Store.themeMode(c))), INDIGO)
            ) { pickMode() }
        )

        // default picker
        content.addView(Ui.sectionTitle(c, getString(R.string.set_default)))
        val fileDef = isDefault(filePickerIntent())
        content.addView(
            Ui.rowView(
                c, content,
                Row(
                    R.drawable.ic_folder, getString(R.string.default_files), getString(R.string.default_files_sub), BLUE,
                    true, getString(if (fileDef) R.string.status_default else R.string.status_not_set),
                    if (fileDef) GREEN else GRAY
                )
            ) { makeDefault(filePickerIntent()) }
        )
        val photoDef = isDefault(photoPickerIntent())
        content.addView(
            Ui.rowView(
                c, content,
                Row(
                    R.drawable.ic_image, getString(R.string.default_photos), getString(R.string.default_photos_sub), PURPLE,
                    true, getString(if (photoDef) R.string.status_default else R.string.status_not_set),
                    if (photoDef) GREEN else GRAY
                )
            ) { makeDefault(photoPickerIntent()) }
        )
        content.addView(
            Ui.rowView(
                c, content,
                Row(R.drawable.ic_settings, getString(R.string.default_apps), getString(R.string.default_apps_sub), GRAY)
            ) { openDefaultApps() }
        )
        content.addView(
            Ui.rowView(
                c, content,
                Row(R.drawable.ic_info, getString(R.string.diagnose), getString(R.string.diagnose_sub), GRAY)
            ) {
                AlertDialog.Builder(c)
                    .setTitle(R.string.diagnose)
                    .setMessage(Defaults.report(c))
                    .setPositiveButton(R.string.done, null)
                    .show()
            }
        )
        content.addView(Ui.footer(c, getString(R.string.default_note)))

        // browser options
        content.addView(Ui.sectionTitle(c, getString(R.string.set_browser)))
        val sortKeys = listOf("date", "name", "size")
        val sortNames = listOf(getString(R.string.sort_date), getString(R.string.sort_name), getString(R.string.sort_size))
        val sortIdx = Math.max(0, sortKeys.indexOf(Store.str(c, "sort", "date")))
        content.addView(
            Ui.rowView(c, content, Row(R.drawable.ic_sort, getString(R.string.sort_by), sortNames[sortIdx], BLUE)) {
                Ui.choose(c, R.string.sort_by, sortNames, sortIdx) { i ->
                    Store.setStr(c, "sort", sortKeys[i])
                    render()
                }
            }
        )
        val cols = Store.int(c, "cols", 4)
        content.addView(
            Ui.rowView(c, content, Row(R.drawable.ic_aspect, getString(R.string.columns), cols.toString(), TEAL)) {
                Ui.choose(c, R.string.columns, listOf("3", "4", "5", "6"), cols - 3) { i ->
                    Store.setInt(c, "cols", i + 3)
                    render()
                }
            }
        )
        val hidden = Ui.toggle(c, content, R.string.show_hidden, R.string.show_hidden_sub, Store.bool(c, "hidden", false), R.drawable.ic_eye, ORANGE)
        hidden.onChange { Store.setBool(c, "hidden", it) }
        content.addView(hidden.view)
        val openCn = ComponentName(c, OpenActivity::class.java)
        val handling = packageManager.getComponentEnabledSetting(openCn) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        val handle = Ui.toggle(c, content, R.string.handle_open, R.string.handle_open_sub, handling, R.drawable.ic_open, GREEN)
        handle.onChange { on ->
            packageManager.setComponentEnabledSetting(
                openCn,
                if (on) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        }
        content.addView(handle.view)

        // linked apps
        content.addView(Ui.sectionTitle(c, getString(R.string.set_link)))
        for (cat in Mimes.CATS) {
            val pkg = Store.linked(c, cat.id)
            content.addView(
                Ui.rowView(c, content, Row(cat.icon, getString(cat.title), appLabel(cat, pkg), cat.tint)) { pickApp(cat) }
            )
        }
        content.addView(Ui.footer(c, getString(R.string.link_note)))

        // permissions
        content.addView(Ui.sectionTitle(c, getString(R.string.set_access)))
        content.addView(
            Ui.rowView(
                c, content,
                Row(R.drawable.ic_shield, getString(R.string.set_media_access), getString(accessText()), GREEN)
            ) { if (Perms.hasMedia(c)) Perms.openAppSettings(c) else requestMedia() }
        )
        content.addView(
            Ui.rowView(
                c, content,
                Row(
                    R.drawable.ic_lock, getString(R.string.set_all_files), getString(R.string.set_all_files_sub), RED,
                    true, getString(if (Perms.hasAllFiles(c)) R.string.access_granted else R.string.access_denied),
                    if (Perms.hasAllFiles(c)) GREEN else GRAY
                )
            ) { requestAllFiles() }
        )
        content.addView(
            Ui.rowView(
                c, content,
                Row(
                    R.drawable.ic_package, getString(R.string.set_install), getString(R.string.set_install_sub), ORANGE,
                    true, getString(if (Perms.canInstall(c)) R.string.access_granted else R.string.access_denied),
                    if (Perms.canInstall(c)) GREEN else GRAY
                )
            ) { Perms.openInstall(c) }
        )
        content.addView(
            Ui.rowView(
                c, content,
                Row(R.drawable.ic_info, getString(R.string.set_app_perms), getString(R.string.set_app_perms_sub), GRAY)
            ) { Perms.openAppSettings(c) }
        )

        // about
        content.addView(Ui.sectionTitle(c, getString(R.string.set_about)))
        content.addView(
            Ui.rowView(
                c, content,
                Row(R.drawable.ic_info, getString(R.string.app_name), getString(R.string.set_version, BuildConfig.VERSION_NAME), GRAY, false),
                null
            )
        )
        content.addView(Ui.footer(c, getString(R.string.about_note)))
    }

    private fun pickMode() {
        val codes = listOf(Appearance.AUTO, Appearance.LIGHT, Appearance.DARK)
        val names = listOf(getString(R.string.theme_auto), getString(R.string.theme_light), getString(R.string.theme_dark))
        Ui.choose(this, R.string.theme_title, names, codes.indexOf(Store.themeMode(this))) { i ->
            Store.setThemeMode(this, codes[i])
            Appearance.apply(this)
        }
    }
}
