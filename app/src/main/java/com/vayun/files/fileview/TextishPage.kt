package com.vayun.files.fileview

import android.content.ContentResolver
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.NavController
import com.vayun.files.MimeType
import com.vayun.files.R
import com.vayun.files.ShareFAB
import com.vayun.files.SimpleIconButton
import com.vayun.files.getMimeType
import dev.snipme.highlights.Highlights
import dev.snipme.highlights.model.BoldHighlight
import dev.snipme.highlights.model.CodeHighlight
import dev.snipme.highlights.model.ColorHighlight
import dev.snipme.highlights.model.SyntaxLanguage
import dev.snipme.highlights.model.SyntaxThemes
import org.json.JSONObject
import java.io.File
import java.io.InputStream


private enum class TextishType {
    REGULAR,
    CODE,
}

private fun mimeToTextishType(mimeType: MimeType): TextishType {
    return when(mimeType) {
        MimeType.JSON,
        MimeType.PYTHON,
        MimeType.JAVA,
        MimeType.C,
        MimeType.CPP,
        MimeType.CSHARP,
        MimeType.DART,
        MimeType.KOTLIN,
        MimeType.RUBY,
        MimeType.SHELL,
        MimeType.SWIFT,
        MimeType.JAVASCRIPT,
        MimeType.GO,
        MimeType.PHP -> TextishType.CODE
        else -> TextishType.REGULAR
    }
}

private fun mimeToLanguage(mimeType: MimeType): SyntaxLanguage {
    return when(mimeType) {
        MimeType.PYTHON -> SyntaxLanguage.PYTHON
        MimeType.JAVA -> SyntaxLanguage.JAVA
        MimeType.C -> SyntaxLanguage.C
        MimeType.CPP -> SyntaxLanguage.CPP
        MimeType.CSHARP -> SyntaxLanguage.CSHARP
        MimeType.DART -> SyntaxLanguage.DART
        MimeType.KOTLIN -> SyntaxLanguage.KOTLIN
        MimeType.RUBY -> SyntaxLanguage.RUBY
        MimeType.SHELL -> SyntaxLanguage.SHELL
        MimeType.SWIFT -> SyntaxLanguage.SWIFT
        MimeType.JAVASCRIPT -> SyntaxLanguage.JAVASCRIPT
        MimeType.GO -> SyntaxLanguage.GO
        MimeType.PHP -> SyntaxLanguage.PHP
        MimeType.JSON -> SyntaxLanguage.DEFAULT
        else -> error("Unsupported language")
    }
}

private fun prettifyJSON(text: String): String {
    val json = JSONObject(text)
    return json.toString(4)
}

private fun supportsFormatting(mimeType: MimeType): Boolean {
    return when(mimeType) {
        MimeType.JSON -> true
        else -> false
    }
}

fun InputStream.readText(): String {
    return this.bufferedReader().use { it.readText() }
}

fun ContentResolver.getText(path: String): String {
    return openInputStream(path.toContentUri())!!.readText()
}

fun String.toContentUri(): Uri {
    return if(startsWith("/")) {
        Uri.fromFile(File(this))
    } else {
        this.toUri()
    }
}

fun ContentResolver.writeText(path: String, text: String) {
    openOutputStream(path.toContentUri())!!.bufferedWriter().use { it.write(text) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextishPage(navController: NavController, path: String) {
    val contentResolver = LocalContext.current.contentResolver
    val currentFile = File(path)
    var originalText by remember { mutableStateOf(contentResolver.getText(path)) }
    val type = mimeToTextishType(getMimeType(currentFile))
    var text by remember { mutableStateOf(originalText) }

    val darkTheme = isSystemInDarkTheme()
    fun getContent(text: String): AnnotatedString {
        return when(type) {
            TextishType.CODE -> {
                val highlights = Highlights.Builder()
                    .code(text)
                    .theme(SyntaxThemes.default(darkTheme))
                    .language(mimeToLanguage(getMimeType(currentFile))).build()
                buildAnnotatedString(text, highlights.getHighlights())
            }
            TextishType.REGULAR -> AnnotatedString(text)
        }
    }
    var oldContext by remember { mutableStateOf(getContent(text))}
    var value by remember { mutableStateOf(TextFieldValue(oldContext)) }

    Scaffold(
        Modifier.imePadding(),
        topBar = { TopAppBar({Text(currentFile.name)}, actions = {
            if(supportsFormatting(getMimeType(currentFile))) {
                SimpleIconButton(R.drawable.outline_format_letter_spacing_2_24) {
                    text = prettifyJSON(text)
                    oldContext = getContent(text)
                    value = value.copy(oldContext)
                }
            }
        }) },
        floatingActionButton = {
            Column {
                AnimatedVisibility(text != originalText) {
                    FloatingActionButton({
                        contentResolver.writeText(path, text)
                        originalText = text
                    }) {
                        Icon(painterResource(R.drawable.outline_save_24), null)
                    }
                }
                Spacer(Modifier.height(6.dp))
                ShareFAB(path)
            }
        }
    ) { innerPadding ->
        Column(Modifier.padding(innerPadding).padding(horizontal = 16.dp)) {
            OutlinedTextField(value, onValueChange = {
                if(it.text != text) oldContext = getContent(it.text)
                text = it.text
                value = it.copy(oldContext)
                }, Modifier.fillMaxSize())
        }
    }
}

fun buildAnnotatedString(
    text: String,
    highlights: List<CodeHighlight>
): AnnotatedString {
    return AnnotatedString.Builder().apply {
        withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) {
            var currentIndex = 0

            // Sort highlights by start index to apply in order
            val sortedHighlights = highlights.sortedBy { it.location.start }

            for (highlight in sortedHighlights) {
                val start = highlight.location.start.coerceIn(0, text.length)
                val end = highlight.location.end.coerceIn(0, text.length)

                // Append unstyled text before this highlight
                if (currentIndex < start) {
                    append(text.substring(currentIndex, start))
                }

                // Apply style for this highlight
                val spanStyle = when (highlight) {
                    is BoldHighlight -> SpanStyle(fontWeight = FontWeight.Bold)
                    is ColorHighlight -> SpanStyle(color = Color(highlight.rgb.or(0xFF000000.toInt())))
                }

                withStyle(spanStyle) {
                    append(text.substring(start, end))
                }

                currentIndex = end
            }

            // Append any remaining text
            if (currentIndex < text.length) {
                append(text.substring(currentIndex))
            }
        }
    }.toAnnotatedString()
}