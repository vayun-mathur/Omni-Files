package com.vayun.files.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.io.File

class CopyWork(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        return try {
            val filePaths = inputData.getStringArray("filePaths")!!
            val destPath = inputData.getString("destPath")!!

            // Validate input: If filePaths is null or empty, or destPath is null or empty, return failure
            if (filePaths.isEmpty()) {
                return Result.failure()
            }

            val destinationDir = File(destPath)
            if (!destinationDir.exists()) {
                destinationDir.mkdirs()
            }

            // Iterate through each source file or directory path
            for (filePath in filePaths) {
                val sourceFileOrDir = File(filePath)

                // Check if the source file or directory actually exists
                if (!sourceFileOrDir.exists()) {
                    println("Source file or directory does not exist, skipping: ${sourceFileOrDir.absolutePath}")
                    continue // Skip to the next item
                }

                // Create the destination path by combining the destination directory and the source item's name
                // For directories, this will create a new directory inside destinationDir
                // For files, this will create a new file inside destinationDir
                val destinationItem = File(destinationDir, sourceFileOrDir.name)

                // Copy the source file or directory to the destination.
                // The copyTo function in Kotlin handles both files and directories recursively.
                // overwrite = true ensures that if an item with the same name already exists, it will be replaced.
                sourceFileOrDir.copyTo(destinationItem, overwrite = true)

                if (sourceFileOrDir.isDirectory) {
                    println("Copied directory ${sourceFileOrDir.absolutePath} to ${destinationItem.absolutePath} recursively.")
                } else {
                    println("Copied file ${sourceFileOrDir.absolutePath} to ${destinationItem.absolutePath}.")
                }
            }

            // If all items are processed without critical errors, return success
            println("All specified files and directories copied successfully to: $destPath")
            Result.success()
        } catch (e: Exception) {
            // Catch any exceptions during the copy process, print the stack trace, and return failure
            e.printStackTrace()
            println("File/Directory copying failed: ${e.message}")
            Result.failure()
        }
    }
}
