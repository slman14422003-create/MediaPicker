package app.mediapicker

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

/** Base of every screen: system bar icons follow day / night, insets are applied, permission and system picker helpers. */
abstract class BaseActivity : AppCompatActivity() {
    private val permLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        onAccessChanged()
    }

    private val systemLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode == RESULT_OK) {
            val d = r.data
            val list = ArrayList<Uri>()
            val cd = d?.clipData
            if (cd != null) {
                for (i in 0 until cd.itemCount) {
                    val u = cd.getItemAt(i).uri
                    if (u != null) list.add(u)
                }
            }
            if (list.isEmpty()) {
                val u = d?.data
                if (u != null) list.add(u)
            }
            if (list.isNotEmpty()) onSystemPicked(list)
        }
    }

    protected open fun onAccessChanged() {}

    protected open fun onSystemPicked(uris: List<Uri>) {}

    protected fun setRoot(v: View) {
        setContentView(v)
        ViewCompat.setOnApplyWindowInsetsListener(v) { view, insets ->
            val b = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            view.setPadding(b.left, b.top, b.right, b.bottom)
            WindowInsetsCompat.CONSUMED
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        try {
            val c = WindowCompat.getInsetsController(window, window.decorView)
            val light = !Appearance.isNight(this)
            c.isAppearanceLightStatusBars = light
            c.isAppearanceLightNavigationBars = light
        } catch (e: Throwable) {
        }
    }

    fun requestMedia() {
        permLauncher.launch(Perms.mediaPerms())
    }

    fun requestAllFiles() {
        if (Build.VERSION.SDK_INT >= 30) Perms.openAllFiles(this) else requestMedia()
    }

    /** The system document browser, for places this app cannot list itself. Never routes back to this app. */
    fun browseSystem(multiple: Boolean, types: Array<String>?) {
        val i = Intent(Intent.ACTION_OPEN_DOCUMENT)
        i.addCategory(Intent.CATEGORY_OPENABLE)
        i.type = "*/*"
        if (multiple) i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        if (types != null && !(types.size == 1 && types[0] == "*/*")) i.putExtra(Intent.EXTRA_MIME_TYPES, types)
        val ch = Intent.createChooser(i, getString(R.string.browse_system))
        ch.putExtra(
            Intent.EXTRA_EXCLUDE_COMPONENTS,
            arrayOf<Parcelable>(ComponentName(this, PickerActivity::class.java))
        )
        try {
            systemLauncher.launch(ch)
        } catch (e: Exception) {
            Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show()
        }
    }
}
