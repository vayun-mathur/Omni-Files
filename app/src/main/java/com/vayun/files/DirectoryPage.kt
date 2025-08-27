package com.vayun.files

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.vayun.files.workers.CopyWork
import com.vayun.files.workers.DeleteWork
import com.vayun.files.workers.ZipWork
import java.io.File

var copiedFiles by mutableStateOf(listOf<String>())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectoryPage(navController: NavController, path: String) {
    val currentFile = File(path)
    var selectedFiles by remember {mutableStateOf(listOf<File>())}
    val context = LocalContext.current

    var currentlyEditingFile: File? by remember { mutableStateOf(null)}

    Scaffold(
        topBar = { TopAppBar({Text(getFileName(currentFile))}, actions = {
            if(copiedFiles.isEmpty()) {
                if(selectedFiles.size == 1) {
                    val selectedFile = selectedFiles.first()
                    SimpleIconButton(R.drawable.outline_highlight_text_cursor_24){
                        currentlyEditingFile = selectedFile
                    }
                    if(!selectedFiles.first().isDirectory) {
                        SimpleIconButton(Icons.Default.Share){
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = getMimeType(selectedFile).value
                                putExtra(
                                    Intent.EXTRA_STREAM,
                                    FileProvider.getUriForFile(
                                        context,
                                        context.applicationContext.packageName + ".provider",
                                        selectedFile
                                    )
                                )
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share File"))
                        }
                    }
                }
                if (selectedFiles.isNotEmpty()) {
                    SimpleIconButton(R.drawable.outline_content_copy_24){
                        copiedFiles = selectedFiles.map { it.path }
                        selectedFiles = listOf()
                    }
                    SimpleIconButton(R.drawable.outline_folder_zip_24){
                        makeWorkRequest<ZipWork>(context) {
                            putStringArray(
                                "originPath",
                                selectedFiles.map { it.path }.toTypedArray()
                            )
                            putString("destPath", File(currentFile, "archive.zip").path)
                        }
                        selectedFiles = listOf()
                    }
                    SimpleIconButton(Icons.Default.Delete){
                        makeWorkRequest<DeleteWork>(context) {
                            putStringArray(
                                "filePaths",
                                selectedFiles.map { it.path }.toTypedArray()
                            )
                        }
                        selectedFiles = listOf()
                    }
                } else {
                    SimpleIconButton(Icons.Default.Add){
                        var counter = 1
                        var newFile = File(currentFile, "Untitled.txt")
                        while (newFile.exists()) {
                            counter++
                            newFile = File(currentFile, "Untitled$counter.txt")
                        }
                        newFile.createNewFile()
                        currentlyEditingFile = newFile
                    }
                }
            } else {
                SimpleIconButton(R.drawable.outline_content_paste_24){
                    makeWorkRequest<CopyWork>(context) {
                        putStringArray(
                            "filePaths",
                            copiedFiles.toTypedArray()
                        )
                        putString("destPath", currentFile.path)
                    }
                    copiedFiles = listOf()
                }
                SimpleIconButton(R.drawable.outline_cancel_24){
                    copiedFiles = listOf()
                }
            }
        }) },
    ) { innerPadding ->
        Column(Modifier.padding(innerPadding)) {
            SharedDirectoryPage(
                currentFile, currentlyEditingFile = currentlyEditingFile, finishRenaming = {currentlyEditingFile = null}, selectedFiles = selectedFiles,
                onSelect = { toSelect ->
                    selectedFiles = if(toSelect in selectedFiles) {
                        selectedFiles.filter { it != toSelect }
                    } else {
                        selectedFiles + toSelect
                    }
                },
                onClick = { child ->
                    val fileType = FileType.of(child)
                    when (fileType) {
                        FileType.DIRECTORY -> {
                            navController.navigate(DirectoryPath(child.path))
                        }

                        FileType.IMAGE -> {
                            navController.navigate(ImagePath(child.path))
                        }

                        FileType.TEXTISH -> {
                            navController.navigate(TextishPath(child.path))
                        }

                        FileType.AUDIO -> {
                            navController.navigate(AudioVideoPath(child.path))
                        }

                        FileType.VIDEO -> {
                            navController.navigate(AudioVideoPath(child.path))
                        }

                        FileType.PDF -> {
                            navController.navigate(PDFPath(child.path))
                        }

                        FileType.ZIP -> {
                            navController.navigate(ZipDestination(child.path, currentFile.path))
                        }

                        FileType.CALENDAR -> {
                            navController.navigate(CalendarPath(child.path))
                        }

                        FileType.CONTACT -> {
                            navController.navigate(ContactPath(child.path))
                        }

                        FileType.SPREADSHEET -> {
                            navController.navigate(SpreadsheetPath(child.path))
                        }

                        FileType.APK -> {
                            val intent = Intent(Intent.ACTION_INSTALL_PACKAGE)
                            intent.data = FileProvider.getUriForFile(
                                context,
                                context.applicationContext.packageName + ".provider",
                                child
                            )
                            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
                            intent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true);
                            intent.putExtra(Intent.EXTRA_RETURN_RESULT, true);
                            intent.putExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME,
                                context.applicationInfo.packageName)
                            context.startActivity(intent)
                        }

                        FileType.UNKNOWN -> {
                            // try to open the file with an intent
                            val intent = Intent(Intent.ACTION_VIEW)
                            intent.data = FileProvider.getUriForFile(
                                context,
                                context.applicationContext.packageName + ".provider",
                                child
                            )
                            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
                            context.startActivity(intent)
                        }
                    }
                },
            )
        }
    }
}