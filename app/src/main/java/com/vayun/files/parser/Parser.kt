package com.vayun.files.parser

import android.content.ContentResolver
import android.net.Uri
import com.vayun.files.fileview.getText
import com.vayun.files.fileview.readText
import java.io.File
import java.io.InputStream

abstract class Parser<T> {
    open fun parse(input: String): T {
        throw NotImplementedError("parse(String) not implemented in ${this::class.simpleName}")
    }
    open fun parse(f: InputStream): T {
        return parse(f.readText())
    }
}