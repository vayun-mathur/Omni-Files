package com.vayun.files

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import androidx.pdf.PdfDocument
import androidx.pdf.SandboxedPdfLoader
import androidx.pdf.compose.PdfViewer
import androidx.pdf.compose.PdfViewerState
import com.vayun.files.fileview.toContentUri
import com.vayun.files.ui.theme.FilesTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        var permissionGranted by mutableStateOf(Environment.isExternalStorageManager())
        val storagePermissionResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            if (Environment.isExternalStorageManager()) {
                permissionGranted = true
            }
        }

        val intent = getIntent()
        val action = intent.action
        val type = intent.type

        setContent {
            FilesTheme {
                if(!permissionGranted) {
                    Scaffold(Modifier.fillMaxSize()) { innerPadding ->
                        Column(
                            Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Card() {
                                Button({
                                    val intent = Intent(
                                        ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                                        "package:$packageName".toUri()
                                    )

                                    storagePermissionResultLauncher.launch(intent)
                                }) {
                                    Text("Request Full Files Permission")
                                }
                            }
                        }
                    }
                } else {
                    if(Intent.ACTION_VIEW == action && type != null) {
                        val type = FileType.of(getMimeType(type))
                        val filePath = intent.dataString!!
                        // convert content:/ path to file:// path
                        when(type) {
                            FileType.DIRECTORY -> Navigation(DirectoryPath(filePath))
                            FileType.IMAGE -> Navigation(ImagePath(filePath))
                            FileType.TEXTISH -> Navigation(TextishPath(filePath))
                            FileType.AUDIO -> Navigation(AudioVideoPath(filePath))
                            FileType.VIDEO -> Navigation(AudioVideoPath(filePath))
                            FileType.PDF -> Navigation(PDFPath(filePath))
                            FileType.ZIP -> Navigation(ZipDestination(filePath, File(filePath).parent!!))
                            FileType.CALENDAR -> Navigation(CalendarPath(filePath))
                            FileType.CONTACT -> Navigation(ContactPath(filePath))
                            FileType.SPREADSHEET -> Navigation(SpreadsheetPath(filePath))
                            FileType.APK -> error("This app shouldn't be openable with this type")
                            FileType.UNKNOWN -> error("This app shouldn't be openable with this type")
                        }
                    } else {
                        Navigation()
                    }
                }
            }
        }
    }
}

fun percentage(curr: Int, count: Int): Float {
    return curr.toFloat()/count.toFloat() * 100
}

@Composable
fun ShareFAB(path: String) {
    val context = LocalContext.current
    val uri = if(path.startsWith("content:/"))
        path.toUri()
    else
        FileProvider.getUriForFile(context, context.applicationContext.packageName + ".provider", File(path))
    ShareFAB(uri)
}

@Composable
fun ShareFAB(uri: Uri) {
    val context = LocalContext.current
    FloatingActionButton({
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "image/*"
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
        context.startActivity(Intent.createChooser(shareIntent, "Share Image"))
    }) {
        Icon(Icons.Default.Share, null)
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PDFPage(navController: NavController, path: String) {
    val currentFile = File(path)
    val loader = SandboxedPdfLoader(LocalContext.current)
    var document: PdfDocument? by remember {mutableStateOf(null)}
    LaunchedEffect(Unit) {
        document = loader.openDocument(path.toContentUri())
    }
    Scaffold(
        topBar = { TopAppBar({Text(getFileName(currentFile))}) },
        floatingActionButton = { ShareFAB(path) }
    ) { innerPadding ->
        Column(Modifier.padding(innerPadding)) {
            val state = remember { PdfViewerState() }
            PdfViewer(document, state)
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioVideoPage(navController: NavController, path: String) {
    val context = LocalContext.current
    val currentFile = File(path)

    var position by rememberSaveable { mutableLongStateOf(0) }

    val exoPlayer = ExoPlayer.Builder(context).build()
    val mediaItem = MediaItem.fromUri(currentFile.toUri())

    DisposableEffect(Unit) {
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.play()
        exoPlayer.seekTo(position)

        onDispose {
            exoPlayer.release()
        }
    }

    DisposableEffect(Unit) {
        val coroutine = CoroutineScope(Dispatchers.Main).launch {
            while(true) {
                delay(100)
                position = exoPlayer.currentPosition
            }
        }
        onDispose {
            coroutine.cancel()
        }
    }
    Scaffold(
        topBar = { TopAppBar({Text(currentFile.name)}) },
        floatingActionButton = { ShareFAB(path) }
    ) { innerPadding ->
        Column(Modifier
            .padding(innerPadding)
            .fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            AndroidView(
                { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                    }
                },
                Modifier.fillMaxSize()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImagePage(navController: NavController, path: String) {
    val currentFile = File(path)
    val bitmap = remember { BitmapFactory.decodeFile(path) }
    Scaffold(
        topBar = { TopAppBar({Text(currentFile.name)}) },
        floatingActionButton = { ShareFAB(path) }
    ) { innerPadding ->
        Column(Modifier.padding(innerPadding)) {
            Image(bitmap.asImageBitmap(), null)
        }
    }
}