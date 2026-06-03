package com.example.data.model

import android.graphics.drawable.Drawable

data class AppInfo(
    val appName: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val sizeBytes: Long,
    val isSystemApp: Boolean,
    val firstInstallTime: Long,
    val lastUpdateTime: Long,
    val targetSdkVersion: Int,
    var isFavorite: Boolean = false
)

data class ApkDetail(
    val label: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val sizeBytes: Long,
    val permissions: List<String>,
    val targetSdkVersion: Int,
    val minSdkVersion: Int,
    val mainActivityName: String?,
    val isVerified: Boolean = true,
    val issuesFound: Int = 0
)

data class PreferenceFile(
    val name: String,
    val sizeBytes: Long,
    val keysCount: Int
)

data class PreferenceEntry(
    val key: String,
    val value: String,
    val type: String // String, Int, Boolean, Long, Float
)

data class FileItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val lastModified: Long,
    val extension: String = ""
)

data class DatabaseInfo(
    val name: String,
    val sizeBytes: Long,
    val tables: List<String> = emptyList()
)

data class DatabaseTableRow(
    val columns: List<String>,
    val cells: List<String>
)
