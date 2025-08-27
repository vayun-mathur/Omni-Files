package com.vayun.files.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ZipWork(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // Retrieve the list of paths to be zipped and the destination path for the zip file
            val originPaths = inputData.getStringArray("originPath")
            val destPath = inputData.getString("destPath")

            // If no origin paths are provided, it's a failure
            if (originPaths.isNullOrEmpty()) {
                println("No origin paths provided for zipping.")
                return Result.failure()
            }
            // If destination path is not provided, it's a failure
            if (destPath.isNullOrEmpty()) {
                println("No destination path provided for zipping.")
                return Result.failure()
            }

            // Create the File object for the destination zip file
            val zipFile = File(destPath)
            // Ensure parent directories exist for the zip file
            zipFile.parentFile?.mkdirs()

            // Initialize ZipOutputStream to write the zip file
            // Use 'use' to ensure the stream is closed automatically
            ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                // Iterate through each path provided in originPaths
                for (filePath in originPaths) {
                    val fileToZip = File(filePath)
                    // Call the recursive helper function to add files/folders to the zip
                    zipFileOrFolder(fileToZip, fileToZip.name, zos)
                }
            }

            println("Zipping completed successfully to: $destPath")
            Result.success()
        } catch (e: Exception) {
            // Catch any exceptions that occur during the zipping process and print stack trace
            e.printStackTrace()
            println("Zipping failed: ${e.message}")
            Result.failure()
        }
    }

    /**
     * Recursively adds files and directories to a ZipOutputStream.
     *
     * @param file The current file or directory to add.
     * @param entryName The name of the entry in the zip file, including its path.
     * @param zos The ZipOutputStream to write to.
     */
    private fun zipFileOrFolder(file: File, entryName: String, zos: ZipOutputStream) {
        // If the file does not exist, skip it
        if (!file.exists()) {
            println("Skipping non-existent file: ${file.absolutePath}")
            return
        }

        // If it's a directory
        if (file.isDirectory) {
            // Create a new ZipEntry for the directory.
            // Directories in zip files usually end with a '/'
            val dirEntryName = if (entryName.endsWith("/")) entryName else "$entryName/"
            val zipEntry = ZipEntry(dirEntryName)
            zos.putNextEntry(zipEntry) // Add the directory entry
            zos.closeEntry() // Close the directory entry

            // Get all files and subdirectories within this directory
            val files = file.listFiles()
            if (files != null) {
                // Recursively call this function for each file/subdirectory
                for (childFile in files) {
                    zipFileOrFolder(childFile, dirEntryName + childFile.name, zos)
                }
            }
        } else { // It's a file
            // Create a new ZipEntry for the file with its entryName (relative path)
            val zipEntry = ZipEntry(entryName)
            zos.putNextEntry(zipEntry) // Add the file entry

            // Use FileInputStream to read the file's content
            FileInputStream(file).use { fis ->
                // Copy the content of the file to the ZipOutputStream
                fis.copyTo(zos)
            }
            zos.closeEntry() // Close the file entry
        }
    }
}
