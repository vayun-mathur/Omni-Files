package com.vayun.files

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

@Composable
fun SimpleIconButton(@DrawableRes icon: Int, onClick: () -> Unit) {
    IconButton(onClick) {
        Icon(painterResource(icon), null)
    }
}

@Composable
fun SimpleIconButton(image: ImageVector, onClick: () -> Unit) {
    IconButton(onClick) {
        Icon(image, null)
    }
}

inline fun <reified T: ListenableWorker> makeWorkRequest(context: Context, builder: Data.Builder.()-> Unit) {
    val workRequest = OneTimeWorkRequestBuilder<T>().setInputData(
        Data.Builder().apply {
            builder()
        }.build()
    ).build()
    WorkManager.getInstance(context).enqueue(workRequest)
}