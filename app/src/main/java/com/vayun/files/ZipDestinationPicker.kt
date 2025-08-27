package com.vayun.files

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.vayun.files.workers.UnzipWork
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZipDestinationPicker(navController: NavController, originPath: String, destPath: String) {
    val context = LocalContext.current
    val currentFile = File(destPath)
    Scaffold(topBar = {
        TopAppBar({Text("Extracting Zip")})
    }, bottomBar = {
        BottomAppBar(modifier = Modifier.height(170.dp)) {
            var newFolderName by remember {mutableStateOf("")}
            val isValidFilename = newFolderName.isNotBlank() && "|\\?*<\":>+[]/'".none { newFolderName.contains(it) }
            Column() {
                OutlinedTextField(
                    newFolderName,
                    { newFolderName = it },
                    Modifier.fillMaxWidth(),
                    label = { Text("New Folder Name") },
                    isError = !isValidFilename
                )
                Spacer(Modifier.height(8.dp))
                Row {
                    Button({
                        navController.popBackStack<DirectoryPath>(false)
                    }) {
                        Text("Cancel")
                    }
                    Spacer(Modifier.padding(8.dp))
                    Button({
                        val workRequest = OneTimeWorkRequestBuilder<UnzipWork>().setInputData(
                            Data.Builder()
                                .putString("originPath", originPath)
                                .putString("destPath", File(currentFile, newFolderName).path)
                                .build()
                        ).build()
                        WorkManager.getInstance(context).enqueue(workRequest)
                        navController.popBackStack<DirectoryPath>(false)
                    }, enabled = isValidFilename) {
                        Text("Extract Here")
                    }
                }
            }
        }
    }) { innerPadding ->
        Column(Modifier.padding(innerPadding)) {
            SharedDirectoryPage(
                currentFile,
                filter = {
                    FileType.of(it) == FileType.DIRECTORY
                },
                onClick = { child ->
                    navController.navigate(ZipDestination(originPath, child.path))
                },
            )
        }
    }
}