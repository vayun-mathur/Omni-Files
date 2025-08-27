package com.vayun.files.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.vayun.files.percentage
import java.io.File

class DeleteWork(appContext: Context, workerParams: WorkerParameters): CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val filePaths = inputData.getStringArray("filePaths")!!.map { File(it) }
        val totalCount = filePaths.size
        filePaths.forEachIndexed { idx, file ->
            val percentage = percentage(idx, totalCount)
            println(percentage)
            if(!file.deleteRecursively()) {
                return Result.failure()
            }
        }
        return Result.success()
    }
}