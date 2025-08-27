package com.vayun.files

import android.os.FileObserver
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import java.io.File

private fun getAncestors(file: File): List<File> {
    val ancestors = mutableListOf(file)
    var current = file
    while (current != root) {
        current = current.parentFile!!
        ancestors.add(current)
    }
    return ancestors
}

@Composable
private fun FileIcon(file: File) {
    val drawableID = when(FileType.of(file)) {
        FileType.DIRECTORY -> R.drawable.outline_folder_24
        FileType.IMAGE -> R.drawable.outline_image_24
        FileType.TEXTISH -> R.drawable.outline_text_snippet_24
        FileType.VIDEO -> R.drawable.outline_video_file_24
        FileType.AUDIO -> R.drawable.outline_music_note_24
        FileType.PDF -> R.drawable.outline_picture_as_pdf_24
        FileType.ZIP -> R.drawable.outline_folder_zip_24
        FileType.CALENDAR -> R.drawable.outline_calendar_month_24
        FileType.CONTACT -> R.drawable.outline_contacts_product_24
        FileType.APK -> R.drawable.outline_apk_install_24
        FileType.SPREADSHEET -> R.drawable.outline_table_24
        FileType.UNKNOWN -> return
    }
    Icon(painterResource(drawableID), null)
}

@Composable
fun SharedDirectoryPage(
    currentFile: File,
    currentlyEditingFile: File? = null,
    finishRenaming: ()->Unit = {},
    selectedFiles: List<File> = listOf(),
    onSelect: (File) -> Unit = {},
    filter: (File) -> Boolean = { true },
    onClick: (File) -> Unit,
) {

    var children by remember { mutableStateOf(currentFile.listFiles()?.toList()
        ?.sortedBy { -it.lastModified() } // sort by most recently modified
        ?.sortedBy { !it.isDirectory } // directories go first
        ?: listOf()) }

    val focusRequester = remember { FocusRequester() }

    DisposableEffect(currentFile) {
        val observer = object : FileObserver(currentFile) {
            // set up a file observer to watch this directory on sd card
            override fun onEvent(event: Int, file: String?) {
                if(event.and(FileObserver.CREATE) != 0 || event.and(FileObserver.DELETE) != 0 || event.and(FileObserver.MODIFY) != 0) {
                    children = currentFile.listFiles()?.toList()
                        ?.sortedBy { -it.lastModified() } // sort by most recently modified
                        ?.sortedBy { !it.isDirectory } // directories go first
                        ?: listOf()
                }
            }
        }
        observer.startWatching()

        onDispose {
            observer.stopWatching()
        }
    }
    Column() {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val ancestors = getAncestors(currentFile).reversed()
            ancestors.forEachIndexed { idx, file ->
                if(idx != 0) {
                    Text(">")
                }
                TextButton({
                    onClick(file)
                }) {
                    Text(getFileName(file))
                }
            }
        }
        LazyColumn() {
            itemsIndexed(children.filter(filter)) { idx, child ->
                println(getMimeTypeString(child))
                if (idx != 0) {
                    HorizontalDivider()
                }
                ListItem({
                    if(currentlyEditingFile == child) {
                        var newName by remember { mutableStateOf(getFileName(child)) }
                        OutlinedTextField(newName, {newName = it}, keyboardActions = KeyboardActions(onDone = {
                            child.renameTo(File(child.parentFile, newName))
                            finishRenaming()
                        }), singleLine = true, modifier = Modifier.focusRequester(focusRequester))
                        LaunchedEffect(Unit) {
                            focusRequester.requestFocus()
                        }
                    } else {
                        Text(getFileName(child))
                    }
                }, Modifier.combinedClickable(onLongClick = {
                    onSelect(child)
                }) { onClick(child) }, leadingContent = {
                    FileIcon(child)
                }, tonalElevation = if(child in selectedFiles) ListItemDefaults.Elevation + 1.dp else ListItemDefaults.Elevation)
            }
        }
    }
}