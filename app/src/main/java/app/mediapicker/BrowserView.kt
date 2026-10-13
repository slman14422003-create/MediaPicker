package app.mediapicker

import android.content.Context
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File

private class VH(v: View) : RecyclerView.ViewHolder(v)

/**
 * The whole file browser used by both the launcher screen and the picker: a Media tab (photos, videos, audio from
 * every album) and a Files tab (every folder and every file type), with search, sort and selection.
 */
class BrowserView(private val ctx: Context, private val host: Host) : LinearLayout(ctx) {

    interface Host {
        fun pickMode(): Boolean
        fun multiple(): Boolean
        fun max(): Int
        fun filesTab(): Boolean
        fun allows(kind: Kind): Boolean
        fun accepts(item: Item): Boolean
        fun selectionChanged(count: Int)
        fun pick(items: List<Item>)
        fun open(item: Item)
        fun askMedia()
        fun askAllFiles()
        fun browseSystem()
    }

    private val main = Handler(Looper.getMainLooper())
    private var tab = 0
    private var typeFilter: Kind? = null
    private var album: String? = null
    private var query = ""
    private var loading = false
    private var cols = Store.int(ctx, "cols", 4)
    private val root: File = Environment.getExternalStorageDirectory()
    private var cwd: File = root
    private val sel = LinkedHashMap<String, Item>()
    private var shownMedia: List<Item> = emptyList()
    private var shownFiles: List<Item> = emptyList()
    private var typeVisible = true

    private val segMedia = Ui.chip(ctx, ctx.getString(R.string.tab_media), true)
    private val segFiles = Ui.chip(ctx, ctx.getString(R.string.tab_files), false)
    private val typeChips = ArrayList<TextView>()
    private val typeScroll = HorizontalScrollView(ctx)
    private val typeRow = LinearLayout(ctx)
    private val albumScroll = HorizontalScrollView(ctx)
    private val albumRow = LinearLayout(ctx)
    private val pathBar = LinearLayout(ctx)
    private val pathText = TextView(ctx)
    private val upBtn: ImageButton = Ui.iconButton(ctx, R.drawable.ic_back, ctx.getString(R.string.up)) { goUp() }
    private val search = EditText(ctx)
    private val frame = FrameLayout(ctx)
    private val grid = RecyclerView(ctx)
    private val list = RecyclerView(ctx)
    private val stateBox = LinearLayout(ctx)
    private val emptyView = TextView(ctx)
    private val progress = ProgressBar(ctx)
    private val gridAd = GridAdapter()
    private val filesAd = FilesAdapter()

    init {
        orientation = VERTICAL

        val segRow = LinearLayout(ctx)
        segRow.setPaddingRelative(Ui.dp(ctx, 16), Ui.dp(ctx, 2), Ui.dp(ctx, 16), Ui.dp(ctx, 4))
        segRow.addView(segMedia)
        if (host.filesTab()) segRow.addView(segFiles)
        addView(segRow)
        segMedia.setOnClickListener { setTab(0) }
        segFiles.setOnClickListener { setTab(1) }

        search.hint = ctx.getString(R.string.search)
        search.setSingleLine(true)
        search.imeOptions = EditorInfo.IME_ACTION_SEARCH
        search.setBackgroundResource(R.drawable.bg_input)
        search.setTextColor(Ui.text(ctx))
        search.setHintTextColor(Ui.hint(ctx))
        search.textSize = 15f
        search.setPaddingRelative(Ui.dp(ctx, 16), Ui.dp(ctx, 8), Ui.dp(ctx, 16), Ui.dp(ctx, 8))
        val sp = LayoutParams(-1, Ui.dp(ctx, 40))
        sp.setMargins(Ui.dp(ctx, 16), Ui.dp(ctx, 2), Ui.dp(ctx, 16), Ui.dp(ctx, 6))
        addView(search, sp)
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                query = s?.toString()?.trim() ?: ""
                refresh()
            }
        })

        typeScroll.isHorizontalScrollBarEnabled = false
        typeRow.setPaddingRelative(Ui.dp(ctx, 16), Ui.dp(ctx, 2), Ui.dp(ctx, 16), Ui.dp(ctx, 4))
        typeScroll.addView(typeRow)
        addView(typeScroll, LayoutParams(-1, -2))
        buildTypeChips()

        albumScroll.isHorizontalScrollBarEnabled = false
        albumRow.setPaddingRelative(Ui.dp(ctx, 16), Ui.dp(ctx, 2), Ui.dp(ctx, 16), Ui.dp(ctx, 8))
        albumScroll.addView(albumRow)
        addView(albumScroll, LayoutParams(-1, -2))

        pathBar.orientation = HORIZONTAL
        pathBar.gravity = Gravity.CENTER_VERTICAL
        pathBar.setPaddingRelative(Ui.dp(ctx, 8), 0, Ui.dp(ctx, 16), Ui.dp(ctx, 4))
        pathBar.addView(upBtn, LayoutParams(Ui.dp(ctx, 40), Ui.dp(ctx, 40)))
        pathText.textSize = 14f
        pathText.setTextColor(Ui.text2(ctx))
        pathText.isSingleLine = true
        pathText.ellipsize = android.text.TextUtils.TruncateAt.START
        pathBar.addView(pathText, LayoutParams(0, -2, 1f))
        addView(pathBar, LayoutParams(-1, -2))
        pathBar.visibility = GONE

        grid.layoutManager = GridLayoutManager(ctx, cols)
        grid.setHasFixedSize(true)
        grid.adapter = gridAd
        grid.clipToPadding = false
        frame.addView(grid, FrameLayout.LayoutParams(-1, -1))

        list.layoutManager = LinearLayoutManager(ctx)
        list.adapter = filesAd
        list.visibility = GONE
        frame.addView(list, FrameLayout.LayoutParams(-1, -1))

        stateBox.orientation = VERTICAL
        stateBox.gravity = Gravity.CENTER
        stateBox.setPadding(Ui.dp(ctx, 32), Ui.dp(ctx, 16), Ui.dp(ctx, 32), Ui.dp(ctx, 16))
        stateBox.visibility = GONE
        frame.addView(stateBox, FrameLayout.LayoutParams(-1, -1))

        emptyView.setTextColor(Ui.text2(ctx))
        emptyView.textSize = 15f
        emptyView.gravity = Gravity.CENTER
        emptyView.text = ctx.getString(R.string.empty)
        emptyView.visibility = GONE
        frame.addView(emptyView, FrameLayout.LayoutParams(-1, -1))

        progress.isIndeterminate = true
        progress.indeterminateTintList = android.content.res.ColorStateList.valueOf(Ui.accent(ctx))
        progress.visibility = GONE
        frame.addView(progress, FrameLayout.LayoutParams(Ui.dp(ctx, 40), Ui.dp(ctx, 40), Gravity.CENTER))

        addView(frame, LayoutParams(-1, 0, 1f))
        refresh()
    }

    // ------------------------------------------------------------------ public api

    fun selected(): List<Item> = ArrayList(sel.values)

    fun clearSelection() {
        if (sel.isEmpty()) return
        sel.clear()
        notifySelection()
        host.selectionChanged(0)
    }

    fun onResume() {
        val c = Store.int(ctx, "cols", 4)
        if (c != cols) {
            cols = c
            grid.layoutManager = GridLayoutManager(ctx, cols)
        }
        if (Perms.hasMedia(ctx)) load(true)
        refresh()
    }

    fun handleBack(): Boolean {
        if (!host.pickMode() && sel.isNotEmpty()) {
            clearSelection()
            return true
        }
        if (tab == 1 && cwd.absolutePath != root.absolutePath) {
            goUp()
            return true
        }
        return false
    }

    fun showSort() {
        val keys = listOf("date", "name", "size")
        val names = listOf(ctx.getString(R.string.sort_date), ctx.getString(R.string.sort_name), ctx.getString(R.string.sort_size))
        val cur = keys.indexOf(Store.str(ctx, "sort", "date"))
        Ui.choose(ctx, R.string.sort_by, names, cur) { i ->
            Store.setStr(ctx, "sort", keys[i])
            refresh()
        }
    }

    // ------------------------------------------------------------------ state

    private fun setTab(t: Int) {
        if (tab == t) return
        tab = t
        Ui.setChip(ctx, segMedia, t == 0)
        Ui.setChip(ctx, segFiles, t == 1)
        refresh()
    }

    private fun goUp() {
        if (cwd.absolutePath != root.absolutePath) {
            cwd = cwd.parentFile ?: root
            refresh()
            list.scrollToPosition(0)
        }
    }

    private fun selecting(): Boolean = (host.pickMode() && host.multiple()) || sel.isNotEmpty()

    private fun matches(i: Item): Boolean = query.isEmpty() || i.name.contains(query, ignoreCase = true)

    private fun cmp(): Comparator<Item> = when (Store.str(ctx, "sort", "date")) {
        "name" -> compareBy<Item> { it.name.lowercase() }
        "size" -> compareByDescending<Item> { it.size }
        else -> compareByDescending<Item> { it.modified }
    }

    private fun showOnly(v: View?) {
        grid.visibility = if (v === grid) VISIBLE else GONE
        list.visibility = if (v === list) VISIBLE else GONE
        stateBox.visibility = if (v === stateBox) VISIBLE else GONE
        emptyView.visibility = if (v === emptyView) VISIBLE else GONE
        progress.visibility = if (v === progress) VISIBLE else GONE
    }

    private fun load(force: Boolean) {
        if (loading) return
        if (Repo.loaded && !force) return
        loading = true
        if (!Repo.loaded) showOnly(progress)
        Thread {
            Repo.loadMedia(ctx)
            main.post {
                loading = false
                refresh()
            }
        }.start()
    }

    fun refresh() {
        typeScroll.visibility = if (tab == 0 && typeVisible) VISIBLE else GONE
        pathBar.visibility = if (tab == 1) VISIBLE else GONE
        if (tab == 0) {
            albumScroll.visibility = GONE
            if (!Perms.hasMedia(ctx)) {
                showState(
                    ctx.getString(R.string.state_media_title), ctx.getString(R.string.state_media_msg),
                    ctx.getString(R.string.allow_access), { host.askMedia() }, null, null
                )
                return
            }
            if (!Repo.loaded) {
                load(false)
                showOnly(progress)
                return
            }
            refreshMedia()
        } else {
            albumScroll.visibility = GONE
            if (!Perms.hasAllFiles(ctx)) {
                showState(
                    ctx.getString(R.string.state_files_title), ctx.getString(R.string.state_files_msg),
                    ctx.getString(R.string.allow_all_files), { host.askAllFiles() },
                    ctx.getString(R.string.browse_system), { host.browseSystem() }
                )
                return
            }
            refreshFiles()
        }
    }

    private fun refreshMedia() {
        val base = Repo.media.filter { host.accepts(it) && (typeFilter == null || it.kind == typeFilter) && matches(it) }
        val counts = LinkedHashMap<String, Int>()
        for (i in base) {
            if (i.bucket.isNotEmpty()) counts[i.bucket] = (counts[i.bucket] ?: 0) + 1
        }
        val cur = album
        if (cur != null && !counts.containsKey(cur)) album = null
        buildAlbumChips(counts)
        val a = album
        val src = if (a == null) base else base.filter { it.bucket == a }
        shownMedia = src.sortedWith(cmp())
        gridAd.notifyDataSetChanged()
        showOnly(if (shownMedia.isEmpty()) emptyView else grid)
    }

    private fun refreshFiles() {
        val rel = cwd.absolutePath.removePrefix(root.absolutePath).trimStart('/')
        pathText.text = if (rel.isEmpty()) ctx.getString(R.string.internal_storage)
        else ctx.getString(R.string.internal_storage) + " / " + rel.replace("/", " / ")
        upBtn.visibility = if (cwd.absolutePath != root.absolutePath) VISIBLE else INVISIBLE
        val raw = Repo.listDir(cwd, Store.bool(ctx, "hidden", false))
        shownFiles = raw.filter { (it.kind == Kind.FOLDER || host.accepts(it)) && matches(it) }
            .sortedWith(compareBy<Item> { it.kind != Kind.FOLDER }.then(cmp()))
        filesAd.notifyDataSetChanged()
        showOnly(if (shownFiles.isEmpty()) emptyView else list)
    }

    private fun showState(title: String, msg: String, primary: String, pa: () -> Unit, secondary: String?, sa: (() -> Unit)?) {
        stateBox.removeAllViews()
        val t = TextView(ctx)
        t.text = title
        t.setTextColor(Ui.text(ctx))
        t.textSize = 20f
        t.typeface = Ui.medium()
        t.gravity = Gravity.CENTER
        stateBox.addView(t, LayoutParams(-1, -2))
        val m = TextView(ctx)
        m.text = msg
        m.setTextColor(Ui.text2(ctx))
        m.textSize = 14.5f
        m.setLineSpacing(0f, 1.15f)
        m.gravity = Gravity.CENTER
        val mp = LayoutParams(-1, -2)
        mp.topMargin = Ui.dp(ctx, 8)
        mp.bottomMargin = Ui.dp(ctx, 20)
        stateBox.addView(m, mp)
        val b = Ui.button(ctx, primary, true)
        b.setOnClickListener { pa() }
        stateBox.addView(b, LayoutParams(-1, -2))
        if (secondary != null && sa != null) {
            val b2 = Ui.button(ctx, secondary, false)
            b2.setOnClickListener { sa() }
            val lp = LayoutParams(-1, -2)
            lp.topMargin = Ui.dp(ctx, 10)
            stateBox.addView(b2, lp)
        }
        showOnly(stateBox)
    }

    // ------------------------------------------------------------------ chips

    private fun buildTypeChips() {
        typeRow.removeAllViews()
        typeChips.clear()
        val defs = listOf<Pair<Int, Kind?>>(
            Pair(R.string.f_all, null),
            Pair(R.string.f_photos, Kind.IMAGE),
            Pair(R.string.f_videos, Kind.VIDEO),
            Pair(R.string.f_audio, Kind.AUDIO)
        )
        val allowed = defs.filter { it.second == null || host.allows(it.second!!) }
        typeVisible = allowed.size > 2
        for (d in allowed) {
            val chip = Ui.chip(ctx, ctx.getString(d.first), d.second == typeFilter)
            chip.setOnClickListener {
                typeFilter = d.second
                album = null
                for (k in typeChips.indices) Ui.setChip(ctx, typeChips[k], allowed[k].second == typeFilter)
                refresh()
            }
            typeChips.add(chip)
            typeRow.addView(chip)
        }
    }

    private fun buildAlbumChips(counts: Map<String, Int>) {
        albumRow.removeAllViews()
        if (counts.size < 2) {
            albumScroll.visibility = GONE
            return
        }
        albumScroll.visibility = VISIBLE
        val all = Ui.chip(ctx, ctx.getString(R.string.all_albums), album == null)
        all.setOnClickListener {
            album = null
            refresh()
        }
        albumRow.addView(all)
        for (name in counts.keys.sortedByDescending { counts[it] ?: 0 }) {
            val chip = Ui.chip(ctx, name, name == album)
            chip.setOnClickListener {
                album = name
                refresh()
                grid.scrollToPosition(0)
            }
            albumRow.addView(chip)
        }
    }

    // ------------------------------------------------------------------ taps and selection

    private fun onTap(i: Item) {
        if (i.kind == Kind.FOLDER) {
            cwd = i.file ?: return
            refresh()
            list.scrollToPosition(0)
            return
        }
        if (host.pickMode()) {
            if (host.multiple()) toggle(i) else host.pick(listOf(i))
        } else {
            if (sel.isNotEmpty()) toggle(i) else host.open(i)
        }
    }

    private fun onLong(i: Item) {
        if (i.kind == Kind.FOLDER) return
        if (host.pickMode() && !host.multiple()) return
        toggle(i)
    }

    private fun toggle(i: Item) {
        val k = i.key
        if (sel.containsKey(k)) {
            sel.remove(k)
        } else {
            if (sel.size >= host.max()) return
            sel[k] = i
        }
        notifySelection()
        host.selectionChanged(sel.size)
    }

    private fun notifySelection() {
        if (shownMedia.isNotEmpty()) gridAd.notifyItemRangeChanged(0, shownMedia.size, "sel")
        if (shownFiles.isNotEmpty()) filesAd.notifyItemRangeChanged(0, shownFiles.size, "sel")
    }

    // ------------------------------------------------------------------ adapters

    private inner class GridAdapter : RecyclerView.Adapter<VH>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val tile = Tile(parent.context)
            val gap = Ui.dp(ctx, 1)
            val lp = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.setMargins(gap, gap, gap, gap)
            tile.layoutParams = lp
            return VH(tile)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val m = shownMedia[position]
            val t = holder.itemView as Tile
            val px = Math.max(120, ctx.resources.displayMetrics.widthPixels / cols)
            t.bind(m, px)
            t.setSelection(selecting(), sel.containsKey(m.key))
            t.setOnClickListener { onTap(m) }
            t.setOnLongClickListener {
                onLong(m)
                true
            }
        }

        override fun onBindViewHolder(holder: VH, position: Int, payloads: MutableList<Any>) {
            if (payloads.isEmpty()) {
                onBindViewHolder(holder, position)
                return
            }
            val m = shownMedia[position]
            (holder.itemView as Tile).setSelection(selecting(), sel.containsKey(m.key))
        }

        override fun onViewRecycled(holder: VH) {
            Thumbs.clear((holder.itemView as Tile).thumb)
        }

        override fun getItemCount(): Int = shownMedia.size
    }

    private inner class FilesAdapter : RecyclerView.Adapter<VH>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val row = FileRow(parent.context)
            row.layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            return VH(row)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val m = shownFiles[position]
            val r = holder.itemView as FileRow
            r.bind(m, Ui.dp(ctx, 88))
            r.setSelection(selecting(), sel.containsKey(m.key), m.kind == Kind.FOLDER)
            r.setOnClickListener { onTap(m) }
            r.setOnLongClickListener {
                onLong(m)
                true
            }
        }

        override fun onBindViewHolder(holder: VH, position: Int, payloads: MutableList<Any>) {
            if (payloads.isEmpty()) {
                onBindViewHolder(holder, position)
                return
            }
            val m = shownFiles[position]
            (holder.itemView as FileRow).setSelection(selecting(), sel.containsKey(m.key), m.kind == Kind.FOLDER)
        }

        override fun onViewRecycled(holder: VH) {
            Thumbs.clear((holder.itemView as FileRow).thumb)
        }

        override fun getItemCount(): Int = shownFiles.size
    }
}
