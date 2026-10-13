package app.mediapicker

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.appcompat.app.AlertDialog

/** Everything about making this app the picker the system uses. */
object Defaults {
    fun filePicker(): Intent {
        val i = Intent(Intent.ACTION_GET_CONTENT)
        i.type = "*/*"
        i.addCategory(Intent.CATEGORY_OPENABLE)
        return i
    }

    fun photoPicker(): Intent {
        val i = Intent("android.provider.action.PICK_IMAGES")
        i.type = "image/*"
        return i
    }

    fun isDefault(c: Context, i: Intent): Boolean {
        val ri = c.packageManager.resolveActivity(i, PackageManager.MATCH_DEFAULT_ONLY)
        return ri != null && ri.activityInfo.packageName == c.packageName
    }

    fun openApps(a: Activity) {
        try {
            a.startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
        } catch (e: Exception) {
            Perms.openAppSettings(a)
        }
    }

    fun make(a: Activity, i: Intent) {
        val ri = a.packageManager.resolveActivity(i, PackageManager.MATCH_DEFAULT_ONLY)
        val pkg = ri?.activityInfo?.packageName
        val noDefault = ri == null || ri.activityInfo.name.contains("Resolver", ignoreCase = true)
        when {
            pkg == a.packageName -> openApps(a)
            noDefault -> {
                try {
                    a.startActivity(i)
                } catch (e: Exception) {
                    openApps(a)
                }
            }
            else -> AlertDialog.Builder(a)
                .setTitle(R.string.default_other_title)
                .setMessage(R.string.default_other_msg)
                .setPositiveButton(R.string.open_apps_list) { _, _ ->
                    try {
                        a.startActivity(Intent(Settings.ACTION_MANAGE_ALL_APPLICATIONS_SETTINGS))
                    } catch (e: Exception) {
                        openApps(a)
                    }
                }
                .setNeutralButton(R.string.open_settings) { _, _ ->
                    try {
                        a.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$pkg")))
                    } catch (e: Exception) {
                        openApps(a)
                    }
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }
}
