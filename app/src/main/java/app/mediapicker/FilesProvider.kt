package app.mediapicker

import android.database.Cursor
import android.database.MatrixCursor
import android.os.CancellationSignal
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract.Document
import android.provider.DocumentsContract.Root
import android.provider.DocumentsProvider
import java.io.File
import java.io.FileNotFoundException

/**
 * Adds Media Picker as a storage in the system file picker (side menu), so it shows up inside every app's
 * picker even though Android never lets a normal app outrank the system picker.
 */
class FilesProvider : DocumentsProvider() {
    private val rootDir: File get() = Environment.getExternalStorageDirectory()

    override fun onCreate(): Boolean = true

    override fun queryRoots(projection: Array<out String>?): Cursor {
        val c = MatrixCursor(projection ?: ROOT_COLS)
        val r = c.newRow()
        r.add(Root.COLUMN_ROOT_ID, "root")
        r.add(Root.COLUMN_DOCUMENT_ID, rootDir.absolutePath)
        r.add(Root.COLUMN_TITLE, context?.getString(R.string.app_name) ?: "Media Picker")
        r.add(Root.COLUMN_SUMMARY, context?.getString(R.string.internal_storage))
        r.add(Root.COLUMN_ICON, R.drawable.ic_brand_mark)
        r.add(Root.COLUMN_MIME_TYPES, "*/*")
        r.add(Root.COLUMN_FLAGS, Root.FLAG_LOCAL_ONLY or Root.FLAG_SUPPORTS_IS_CHILD)
        return c
    }

    private fun fileFor(id: String): File {
        val f = File(id).canonicalFile
        val base = rootDir.canonicalPath
        val p = f.path
        if (p != base && !p.startsWith("$base/")) throw FileNotFoundException("outside storage")
        val rel = p.removePrefix(base).trimStart('/')
        if (rel == "Android/data" || rel.startsWith("Android/data/") ||
            rel == "Android/obb" || rel.startsWith("Android/obb/")
        ) throw FileNotFoundException("restricted")
        if (!f.exists()) throw FileNotFoundException("missing")
        return f
    }

    private fun addFile(c: MatrixCursor, f: File) {
        val dir = f.isDirectory
        val r = c.newRow()
        r.add(Document.COLUMN_DOCUMENT_ID, f.absolutePath)
        r.add(Document.COLUMN_DISPLAY_NAME, if (f.absolutePath == rootDir.absolutePath) "Internal storage" else f.name)
        r.add(Document.COLUMN_SIZE, if (dir) 0L else f.length())
        r.add(Document.COLUMN_MIME_TYPE, if (dir) Document.MIME_TYPE_DIR else Mimes.mimeOf(f.name))
        r.add(Document.COLUMN_LAST_MODIFIED, f.lastModified())
        r.add(Document.COLUMN_FLAGS, 0)
    }

    override fun queryDocument(documentId: String, projection: Array<out String>?): Cursor {
        val c = MatrixCursor(projection ?: DOC_COLS)
        addFile(c, fileFor(documentId))
        return c
    }

    override fun queryChildDocuments(parentDocumentId: String, projection: Array<out String>?, sortOrder: String?): Cursor {
        val c = MatrixCursor(projection ?: DOC_COLS)
        val kids = fileFor(parentDocumentId).listFiles() ?: return c
        for (k in kids) {
            if (k.name.startsWith(".")) continue
            try {
                fileFor(k.absolutePath)
                addFile(c, k)
            } catch (e: FileNotFoundException) {
            }
        }
        return c
    }

    override fun isChildDocument(parentDocumentId: String, documentId: String): Boolean =
        documentId == parentDocumentId || documentId.startsWith("$parentDocumentId/")

    override fun openDocument(documentId: String, mode: String, signal: CancellationSignal?): ParcelFileDescriptor {
        if (mode.contains('w') || mode.contains('a') || mode.contains('t')) throw FileNotFoundException("read only")
        return ParcelFileDescriptor.open(fileFor(documentId), ParcelFileDescriptor.MODE_READ_ONLY)
    }

    companion object {
        private val ROOT_COLS = arrayOf(
            Root.COLUMN_ROOT_ID, Root.COLUMN_FLAGS, Root.COLUMN_ICON, Root.COLUMN_TITLE,
            Root.COLUMN_SUMMARY, Root.COLUMN_DOCUMENT_ID, Root.COLUMN_MIME_TYPES
        )
        private val DOC_COLS = arrayOf(
            Document.COLUMN_DOCUMENT_ID, Document.COLUMN_MIME_TYPE, Document.COLUMN_DISPLAY_NAME,
            Document.COLUMN_LAST_MODIFIED, Document.COLUMN_FLAGS, Document.COLUMN_SIZE
        )
    }
}
