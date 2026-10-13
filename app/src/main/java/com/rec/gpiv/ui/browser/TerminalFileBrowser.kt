package com.rec.gpiv.ui.browser

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.FrameLayout
import android.widget.GridView
import android.widget.TextView
import android.widget.Toast
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
    val dialog = AlertDialog.Builder(context, R.style.Theme_Gpiv_FileBrowser)
        .setPositiveButton("Abrir esta pasta") { _, _ -> onSelectFolder() }
        .setNeutralButton("Tags e exportação") { _, _ -> onSelectDocumentTree() }
        .setNegativeButton("Cancelar", null)
        .create()
    val title = TextView(dialog.context).apply {
        text = directory.absolutePath
        typeface = Typeface.MONOSPACE
        textSize = 14f
        setTextColor(Color.WHITE)
        setPadding(dp(16), dp(16), dp(16), dp(12))
        maxLines = 2
        ellipsize = TextUtils.TruncateAt.MIDDLE
    }
    dialog.setCustomTitle(title)
    val grid = GridView(dialog.context).apply {
        setBackgroundColor(Color.BLACK)
        setPadding(dp(8), 0, dp(8), dp(8))
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
                    setPadding(dp(8), dp(12), dp(8), dp(12))
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
            Toast.makeText(context, files[position].name, Toast.LENGTH_LONG).show()
            true
        }
    }
    val container = FrameLayout(dialog.context).apply {
        setBackgroundColor(Color.BLACK)
        addView(grid, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            (context.resources.displayMetrics.heightPixels * 0.65f).toInt()
        ))
    }
    dialog.setView(container)
    dialog.show()
    dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    grid.post {
        // Mantém pelo menos duas colunas; telas maiores comportam mais nomes.
        grid.numColumns = ((grid.width - grid.paddingLeft - grid.paddingRight) / dp(160)).coerceIn(2, 5)
    }
}
