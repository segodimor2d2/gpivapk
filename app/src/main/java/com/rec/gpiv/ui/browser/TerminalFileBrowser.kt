package com.rec.gpiv.ui.browser

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.TextUtils
import android.view.View
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.BaseAdapter
import android.widget.FrameLayout
import android.widget.GridView
import android.widget.TextView
import com.rec.gpiv.R
import java.io.File

/** Lista compacta em colunas, com diretórios marcados por uma barra final. */
fun showTerminalFileBrowser(
    context: Context,
    directory: File,
    entries: List<File>,
    onOpen: (File) -> Unit,
    onSelectFolder: () -> Unit,
    onSelectDocumentTree: () -> Unit
) {
    val density = context.resources.displayMetrics.density
    fun dp(value: Int) = (value * density).toInt()
    val parent = directory.parentFile?.takeIf { directory.absolutePath != "/storage" }
    val files = listOfNotNull(parent) + entries
    val labels = (if (parent != null) listOf("../") else emptyList()) +
        entries.map { it.name + if (it.isDirectory) "/" else "" }
    val dialog = Dialog(context, R.style.Theme_Gpiv_FileBrowser)
    val title = TextView(dialog.context).apply {
        text = directory.absolutePath
        typeface = Typeface.MONOSPACE
        textSize = 14f
        setTextColor(Color.WHITE)
        setPadding(dp(6), dp(6), dp(6), dp(6))
        maxLines = 2
        ellipsize = TextUtils.TruncateAt.MIDDLE
    }
    val nameNotice = TextView(dialog.context).apply {
        visibility = View.GONE
        setBackgroundColor(Color.BLACK)
        setTextColor(Color.WHITE)
        textSize = 13f
        typeface = Typeface.MONOSPACE
        gravity = Gravity.CENTER
        setPadding(dp(12), dp(8), dp(12), dp(8))
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
    }
    val hideNameNotice = Runnable { nameNotice.visibility = View.GONE }
    val grid = GridView(dialog.context).apply {
        setBackgroundColor(Color.BLACK)
        setPadding(dp(6), dp(6), dp(6), dp(6))
        horizontalSpacing = dp(8)
        stretchMode = GridView.STRETCH_COLUMN_WIDTH
        numColumns = 2
        adapter = object : BaseAdapter() {
            override fun getCount() = files.size
            override fun getItem(position: Int) = files[position]
            override fun getItemId(position: Int) = position.toLong()
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
                ((convertView as? TextView) ?: TextView(dialog.context).apply {
                    typeface = Typeface.MONOSPACE
                    textSize = 13f
                    setTextColor(Color.WHITE)
                    setPadding(dp(6), dp(6), dp(6), dp(6))
                    minHeight = dp(48)
                    setSingleLine()
                    ellipsize = TextUtils.TruncateAt.MIDDLE
                }).apply {
                    text = labels[position]
                    contentDescription = if (files[position].isDirectory) "Pasta ${files[position].name}" else files[position].name
                }
        }
        setOnItemClickListener { _, _, position, _ ->
            dialog.dismiss()
            onOpen(files[position])
        }
        setOnItemLongClickListener { _, _, position, _ ->
            nameNotice.removeCallbacks(hideNameNotice)
            nameNotice.text = files[position].name
            nameNotice.visibility = View.VISIBLE
            nameNotice.postDelayed(hideNameNotice, 500L)
            true
        }
    }
    val container = FrameLayout(dialog.context).apply {
        setBackgroundColor(Color.BLACK)
        addView(grid, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ))
    }
    container.addView(nameNotice, FrameLayout.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT,
        Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
    ).apply {
        setMargins(dp(12), 0, dp(12), dp(12))
    })
    dialog.setOnDismissListener {
        nameNotice.removeCallbacks(hideNameNotice)
        nameNotice.visibility = View.GONE
    }
    val footer = LinearLayout(dialog.context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setBackgroundColor(Color.BLACK)
        setPadding(dp(6), dp(4), dp(6), dp(4))
    }
    fun action(label: String, onClick: () -> Unit) {
        footer.addView(Button(dialog.context).apply {
            text = label
            isAllCaps = false
            textSize = 12f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.TRANSPARENT)
            setPadding(dp(4), dp(8), dp(4), dp(8))
            minHeight = dp(48)
            setOnClickListener {
                dialog.dismiss()
                onClick()
            }
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
    }
    action("Tags e exportação", onSelectDocumentTree)
    parent?.let { pastaPai -> action("../") { onOpen(pastaPai) } }
    action("Cancelar") {}
    action("Abrir esta pasta", onSelectFolder)
    val layout = LinearLayout(dialog.context).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(Color.BLACK)
        fitsSystemWindows = true
        addView(title, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        addView(container, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        addView(footer, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }
    dialog.setContentView(layout)
    dialog.show()
    dialog.window?.apply {
        setBackgroundDrawableResource(R.drawable.file_browser_background)
        decorView.clipToOutline = true
        setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }
    grid.post {
        // Mantém pelo menos duas colunas; telas maiores comportam mais nomes.
        grid.numColumns = ((grid.width - grid.paddingLeft - grid.paddingRight) / dp(160)).coerceIn(2, 5)
    }
}
