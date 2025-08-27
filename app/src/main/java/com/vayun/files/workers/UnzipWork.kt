package com.vayun.files.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.vayun.files.percentage
import java.io.File
import java.util.zip.ZipFile

class UnzipWork(appContext: Context, workerParams: WorkerParameters): CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val originPath = inputData.getString("originPath")!!
        val destPath = inputData.getString("destPath")!!
        val zipFile = ZipFile(originPath)
        val entriesCount = zipFile.entries().asSequence().count()
        zipFile.entries().asSequence().forEachIndexed { idx, entry ->
            val percentage = percentage(idx, entriesCount)
            println(percentage)
            val destFile = File(destPath, entry.name)
            if(entry.isDirectory) {
                destFile.mkdirs()
            } else {
                destFile.parentFile!!.mkdirs()
                zipFile.getInputStream(entry).use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
        }
        return Result.success()
    }
}