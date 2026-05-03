package com.diarioestoico.app.ui.components

import android.os.Build
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus

/**
 * Extends the platform floating text-selection toolbar with a "Salvar frase" action.
 * When triggered it copies the selection (via [onCopyRequested]) and then calls
 * [onSavePhrase] with the selected text read from the clipboard.
 */
class StoicTextToolbar(
    private val view: View,
    private val onSavePhrase: () -> Unit
) : TextToolbar {

    private var actionMode: ActionMode? = null

    override var status: TextToolbarStatus = TextToolbarStatus.Hidden
        private set

    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        actionMode?.finish()
        actionMode = view.startActionMode(
            object : ActionMode.Callback2() {
                override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                    if (onCopyRequested != null) {
                        menu.add(Menu.NONE, ID_COPY, 0, android.R.string.copy)
                            .setIcon(android.R.drawable.ic_menu_edit)
                    }
                    menu.add(Menu.NONE, ID_SAVE, 1, "Salvar frase ✦")
                    if (onSelectAllRequested != null) {
                        menu.add(Menu.NONE, ID_SELECT_ALL, 2, android.R.string.selectAll)
                    }
                    return true
                }

                override fun onPrepareActionMode(mode: ActionMode, menu: Menu) = false

                override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                    return when (item.itemId) {
                        ID_COPY -> {
                            onCopyRequested?.invoke()
                            mode.finish()
                            true
                        }
                        ID_SAVE -> {
                            // Copy selection first so the text is available in clipboard
                            onCopyRequested?.invoke()
                            onSavePhrase()
                            mode.finish()
                            true
                        }
                        ID_SELECT_ALL -> {
                            onSelectAllRequested?.invoke()
                            true
                        }
                        else -> false
                    }
                }

                override fun onDestroyActionMode(mode: ActionMode) {
                    status = TextToolbarStatus.Hidden
                    actionMode = null
                }

                override fun onGetContentRect(
                    mode: ActionMode,
                    view: View,
                    outRect: android.graphics.Rect
                ) {
                    outRect.set(
                        rect.left.toInt(),
                        rect.top.toInt(),
                        rect.right.toInt(),
                        rect.bottom.toInt()
                    )
                }
            },
            ActionMode.TYPE_FLOATING
        )
        status = TextToolbarStatus.Shown
    }

    override fun hide() {
        status = TextToolbarStatus.Hidden
        actionMode?.finish()
        actionMode = null
    }

    private companion object {
        const val ID_COPY = 1
        const val ID_SAVE = 2
        const val ID_SELECT_ALL = 3
    }
}
