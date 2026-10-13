package app.mediapicker

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.ContextCompat

object Perms {
    fun mediaPerms(): Array<String> = when {
        Build.VERSION.SDK_INT >= 34 -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
            Manifest.permission.ACCESS_MEDIA_LOCATION
        )
        Build.VERSION.SDK_INT >= 33 -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.ACCESS_MEDIA_LOCATION
        )
        Build.VERSION.SDK_INT >= 29 -> arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_MEDIA_LOCATION
        )
        else -> arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    private fun granted(c: Context, p: String): Boolean =
        ContextCompat.checkSelfPermission(c, p) == PackageManager.PERMISSION_GRANTED

    /** True when at least some media can be read (full access or the "selected photos" choice). */
    fun hasMedia(c: Context): Boolean {
        if (Build.VERSION.SDK_INT >= 33) {
            if (granted(c, Manifest.permission.READ_MEDIA_IMAGES) ||
                granted(c, Manifest.permission.READ_MEDIA_VIDEO) ||
                granted(c, Manifest.permission.READ_MEDIA_AUDIO)
            ) return true
            return Build.VERSION.SDK_INT >= 34 && granted(c, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
        }
        return granted(c, Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    /** True when the user only shared a few items (Android 14 "selected photos"). */
    fun isPartial(c: Context): Boolean = Build.VERSION.SDK_INT >= 34 &&
            !granted(c, Manifest.permission.READ_MEDIA_IMAGES) &&
            !granted(c, Manifest.permission.READ_MEDIA_VIDEO) &&
            granted(c, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)

    /** Access to every folder and file (needed to browse and open all file types). */
    fun hasAllFiles(c: Context): Boolean =
        if (Build.VERSION.SDK_INT >= 30) Environment.isExternalStorageManager()
        else granted(c, Manifest.permission.READ_EXTERNAL_STORAGE)

    fun openAllFiles(a: Activity) {
        if (Build.VERSION.SDK_INT >= 30) {
            try {
                a.startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:" + a.packageName)
                    )
                )
                return
            } catch (e: Exception) {
            }
            try {
                a.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                return
            } catch (e: Exception) {
            }
        }
        openAppSettings(a)
    }

    fun canInstall(c: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || c.packageManager.canRequestPackageInstalls()

    fun openInstall(a: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                a.startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:" + a.packageName)
                    )
                )
                return
            } catch (e: Exception) {
            }
        }
        openAppSettings(a)
    }

    fun openAppSettings(a: Activity) {
        try {
            a.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + a.packageName))
            )
        } catch (e: Exception) {
        }
    }
}
