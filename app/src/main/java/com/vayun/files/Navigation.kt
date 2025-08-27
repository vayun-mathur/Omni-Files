package com.vayun.files

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.vayun.files.fileview.CalendarPage
import com.vayun.files.fileview.ContactPage
import com.vayun.files.fileview.SpreadsheetPage
import com.vayun.files.fileview.TextishPage
import kotlinx.serialization.Serializable
import java.io.File

interface RouteType

@Serializable
data class DirectoryPath(val path: String) : RouteType

@Serializable
data class ImagePath(val path: String) : RouteType

@Serializable
data class TextishPath(val path: String) : RouteType

@Serializable
data class AudioVideoPath(val path: String) : RouteType

@Serializable
data class PDFPath(val path: String) : RouteType

@Serializable
data class ZipDestination(val originPath: String, val destPath: String): RouteType

@Serializable
data class CalendarPath(val path: String) : RouteType

@Serializable
data class ContactPath(val path: String) : RouteType

@Serializable
data class SpreadsheetPath(val path: String) : RouteType

// DirectoryPath(root.path)
// SpreadsheetPath(File(root, "Download/Project-Management-Sample-Data1.ods").path)
// TextishPath(File(root, "Documents/Acode/Cais/Nanopore.py").path)

@Composable
fun Navigation(startDestination: RouteType = DirectoryPath(root.path)) {
    val navController = rememberNavController()
    Surface {
        NavHost(navController, startDestination) {
            composable<DirectoryPath>() {
                DirectoryPage(navController, it.toRoute<DirectoryPath>().path)
            }
            composable<ImagePath> {
                ImagePage(navController, it.toRoute<ImagePath>().path)
            }
            composable<TextishPath> {
                TextishPage(navController, it.toRoute<TextishPath>().path)
            }
            composable<AudioVideoPath> {
                AudioVideoPage(navController, it.toRoute<AudioVideoPath>().path)
            }
            composable<PDFPath> {
                PDFPage(navController, it.toRoute<PDFPath>().path)
            }
            composable<ZipDestination> {
                val route = it.toRoute<ZipDestination>()
                ZipDestinationPicker(navController, route.originPath, route.destPath)
            }
            composable<CalendarPath> {
                CalendarPage(navController, it.toRoute<CalendarPath>().path)
            }
            composable<ContactPath> {
                ContactPage(navController, it.toRoute<ContactPath>().path)
            }
            composable<SpreadsheetPath> {
                SpreadsheetPage(navController, it.toRoute<SpreadsheetPath>().path)
            }
        }
    }
}