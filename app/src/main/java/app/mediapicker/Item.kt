package app.mediapicker

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import java.io.File

enum class Kind { IMAGE, VIDEO, AUDIO, DOC, ARCHIVE, APK, OTHER, FOLDER }

class Item(
    val kind: Kind,
    val name: String,
    val mime: String,
    val size: Long,
    val modified: Long,
    val bucket: String,
    val duration: Long,
    val uri: Uri?,
    val file: File?
) {
    val key: String get() = uri?.toString() ?: (file?.absolutePath ?: name)

    /** Content Uri other apps can read: the MediaStore Uri, or a FileProvider Uri for plain files. */
    fun shareUri(c: Context): Uri =
        uri ?: FileProvider.getUriForFile(c, c.packageName + ".files", file!!)
}

fun Kind.icon(): Int = when (this) {
    Kind.IMAGE -> R.drawable.ic_image
    Kind.VIDEO -> R.drawable.ic_video
    Kind.AUDIO -> R.drawable.ic_music
    Kind.DOC -> R.drawable.ic_file_text
    Kind.ARCHIVE -> R.drawable.ic_archive
    Kind.APK -> R.drawable.ic_package
    Kind.FOLDER -> R.drawable.ic_folder
    Kind.OTHER -> R.drawable.ic_file
}

fun Kind.tint(): Int = when (this) {
    Kind.IMAGE -> 0xFF007AFF.toInt()
    Kind.VIDEO -> 0xFFFF3B30.toInt()
    Kind.AUDIO -> 0xFFFF2D55.toInt()
    Kind.DOC -> 0xFFFF9500.toInt()
    Kind.ARCHIVE -> 0xFF8E8E93.toInt()
    Kind.APK -> 0xFF34C759.toInt()
    Kind.FOLDER -> 0xFF5AC8FA.toInt()
    Kind.OTHER -> 0xFF8E8E93.toInt()
}

class Cat(val id: String, val title: Int, val sample: String, val icon: Int, val tint: Int)

object Mimes {
    val CATS: List<Cat> = listOf(
        Cat("image", R.string.cat_image, "image/jpeg", R.drawable.ic_image, 0xFF007AFF.toInt()),
        Cat("video", R.string.cat_video, "video/mp4", R.drawable.ic_video, 0xFFFF3B30.toInt()),
        Cat("audio", R.string.cat_audio, "audio/mpeg", R.drawable.ic_music, 0xFFFF2D55.toInt()),
        Cat("pdf", R.string.cat_pdf, "application/pdf", R.drawable.ic_file_text, 0xFFFF9500.toInt()),
        Cat("text", R.string.cat_text, "text/plain", R.drawable.ic_text, 0xFF5856D6.toInt()),
        Cat(
            "office", R.string.cat_office,
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            R.drawable.ic_file_text, 0xFF30B0C7.toInt()
        ),
        Cat("archive", R.string.cat_archive, "application/zip", R.drawable.ic_archive, 0xFF8E8E93.toInt()),
        Cat("apk", R.string.cat_apk, "application/vnd.android.package-archive", R.drawable.ic_package, 0xFF34C759.toInt()),
        Cat("other", R.string.cat_other, "*/*", R.drawable.ic_file, 0xFFAF52DE.toInt())
    )

    fun mimeOf(name: String): String {
        val ext = name.substringAfterLast('.', "").lowercase()
        if (ext.isEmpty()) return "application/octet-stream"
        val m = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
        if (m != null) return m
        return when (ext) {
            "mkv" -> "video/x-matroska"
            "7z" -> "application/x-7z-compressed"
            "rar" -> "application/vnd.rar"
            "apk" -> "application/vnd.android.package-archive"
            "flac" -> "audio/flac"
            "opus" -> "audio/ogg"
            "heic", "heif" -> "image/heic"
            "md", "log", "csv", "json", "xml", "yml", "yaml", "ini", "conf", "java", "kt", "py", "js", "sh" -> "text/plain"
            else -> "application/octet-stream"
        }
    }

    private fun isOffice(m: String): Boolean =
        m.contains("officedocument") || m.contains("msword") || m.contains("ms-excel") ||
                m.contains("ms-powerpoint") || m.contains("opendocument") || m.contains("rtf")

    private fun isArchive(m: String): Boolean =
        m == "application/zip" || m.contains("x-zip") || m.contains("rar") || m.contains("7z") ||
                m.contains("x-tar") || m.contains("gzip") || m.contains("x-bzip") || m.contains("x-compress")

    fun kindOf(mime: String, name: String): Kind {
        val m = mime.lowercase()
        return when {
            m.startsWith("image/") -> Kind.IMAGE
            m.startsWith("video/") -> Kind.VIDEO
            m.startsWith("audio/") -> Kind.AUDIO
            m == "application/vnd.android.package-archive" || name.lowercase().endsWith(".apk") -> Kind.APK
            isArchive(m) -> Kind.ARCHIVE
            m == "application/pdf" || m.startsWith("text/") || isOffice(m) ||
                    m == "application/epub+zip" || m == "application/json" || m == "application/xml" -> Kind.DOC
            else -> Kind.OTHER
        }
    }

    fun category(mime: String, name: String): String {
        val m = mime.lowercase()
        return when {
            m.startsWith("image/") -> "image"
            m.startsWith("video/") -> "video"
            m.startsWith("audio/") -> "audio"
            m == "application/pdf" -> "pdf"
            m.startsWith("text/") || m == "application/json" || m == "application/xml" -> "text"
            isOffice(m) -> "office"
            m == "application/vnd.android.package-archive" || name.lowercase().endsWith(".apk") -> "apk"
            isArchive(m) -> "archive"
            else -> "other"
        }
    }
}
