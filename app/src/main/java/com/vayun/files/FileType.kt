package com.vayun.files

import android.os.Build
import android.os.Environment
import android.webkit.MimeTypeMap
import java.io.File

enum class MimeType(val value: String, val regex: String = value) {
    CSV("text/comma-separated-values"),
    TSV("text/tab-separated-values"),
    ODS("application/vnd.oasis.opendocument.spreadsheet"),
    IMAGE("image/*", "image/.*"),
    AUDIO("audio/*", "audio/.*"),
    VIDEO("video/*", "video/.*"),
    PDF("application/pdf"),
    ZIP("application/zip"),
    CALENDAR("text/calendar"),
    CONTACT("text/x-vcard"),
    APK("application/vnd.android.package-archive"),
    JSON("application/json"),
    RUBY("application/x-ruby"),
    PHP("application/x-php"),
    JAVASCRIPT("application/javascript"),
    PYTHON("text/x-python"),
    JAVA("text/x-java"),
    C("text/x-csrc"),
    CPP("text/x-c++src"),
    CSHARP("text/x-csharp"),
    DART("text/x-dart"),
    KOTLIN("text/x-kotlin"),
    SHELL("text/x-sh"),
    SWIFT("text/x-swift"),
    GO("text/x-go"),
    TEXT_OTHER("text/*", "text/.*"),
    UNKNOWN("*/*", ".*")
}

enum class FileType {
    DIRECTORY,
    IMAGE,
    TEXTISH,
    AUDIO,
    VIDEO,
    PDF,
    ZIP,
    CALENDAR,
    CONTACT,
    APK,
    SPREADSHEET,
    UNKNOWN;

    companion object {
        fun of(file: File): FileType {
            if(file.isDirectory) return DIRECTORY
            return of(getMimeType(file))
        }
        fun of(type: MimeType): FileType {
            return when (type) {
                MimeType.IMAGE -> IMAGE
                MimeType.CALENDAR -> CALENDAR
                MimeType.CONTACT -> CONTACT
                MimeType.APK -> APK
                MimeType.TEXT_OTHER, MimeType.JSON, MimeType.RUBY, MimeType.PYTHON,
                MimeType.JAVA, MimeType.C, MimeType.CPP, MimeType.CSHARP, MimeType.DART,
                MimeType.KOTLIN, MimeType.SHELL, MimeType.SWIFT, MimeType.GO, MimeType.JAVASCRIPT,
                MimeType.PHP -> TEXTISH
                MimeType.AUDIO -> AUDIO
                MimeType.VIDEO -> VIDEO
                MimeType.PDF -> PDF
                MimeType.ZIP -> ZIP
                MimeType.CSV, MimeType.TSV, MimeType.ODS -> SPREADSHEET
                MimeType.UNKNOWN -> UNKNOWN
            }
        }
    }
}

val root: File = Environment.getExternalStorageDirectory()

fun getFileName(file: File): String =
    if(file.path == root.path) Build.MODEL else file.name

fun getMimeType(file: File): MimeType =
    getMimeType(getMimeTypeString(file))

fun getMimeType(mimeString: String): MimeType =
    MimeType.entries.first { Regex(it.regex).matches(mimeString) }

fun getMimeTypeString(file: File): String =
    when (file.extension) {
        "cs" -> "text/x-csharp"
        "dart" -> "text/x-dart"
        "kt" -> "text/x-kotlin"
        "swift" -> "text/x-swift"
        "go" -> "text/x-go"
        "php" -> "application/x-php"
        else -> MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(file.extension.lowercase()) ?: "*/*"
    }