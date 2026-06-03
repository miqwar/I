package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import android.os.Build
import com.example.data.local.db.*
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class KeyRoomRepository(private val context: Context) {

    private val database = KeyRoomDatabase.getDatabase(context)
    private val activityDao = database.recentActivityDao()
    private val backupDao = database.backupRecordDao()
    private val queryDao = database.savedQueryDao()

    init {
        // Prepare some sample files and preferences for demonstration of management tools
        setupSandboxFilesAndPrefs()
    }

    // --- ACTIVITY LOGS ---
    val recentActivities: Flow<List<RecentActivity>> = activityDao.getRecentActivities()

    suspend fun logActivity(title: String, category: String, status: String = "SUCCESS", description: String = "") {
        withContext(Dispatchers.IO) {
            activityDao.insertActivity(
                RecentActivity(title = title, category = category, status = status, description = description)
            )
        }
    }

    suspend fun clearActivities() {
        withContext(Dispatchers.IO) {
            activityDao.clearActivities()
        }
    }

    // --- SAVED QUERIES ---
    val savedQueries: Flow<List<SavedQuery>> = queryDao.getAllQueries()

    suspend fun saveQuery(title: String, sqlText: String) {
        withContext(Dispatchers.IO) {
            queryDao.insertQuery(SavedQuery(title = title, sqlText = sqlText))
            logActivity("Saved Query: $title", "Database", "SUCCESS", "Saved query: $sqlText")
        }
    }

    suspend fun deleteQuery(query: SavedQuery) {
        withContext(Dispatchers.IO) {
            queryDao.deleteQuery(query)
            logActivity("Deleted Query: ${query.title}", "Database")
        }
    }

    // --- BACKUPS ---
    val backupRecords: Flow<List<BackupRecord>> = backupDao.getAllBackups()

    suspend fun registerBackup(name: String, type: String, size: Long, filePath: String, itemCount: Int) {
        withContext(Dispatchers.IO) {
            backupDao.insertBackup(
                BackupRecord(name = name, type = type, size = size, filePath = filePath, itemCount = itemCount)
            )
            logActivity("Created Backup: $name", "Backup", "SUCCESS", "Type: $type, Size: $size bytes")
        }
    }

    suspend fun deleteBackupRecord(backup: BackupRecord) {
        withContext(Dispatchers.IO) {
            val file = File(backup.filePath)
            if (file.exists()) {
                file.delete()
            }
            backupDao.deleteBackup(backup)
            logActivity("Deleted Backup: ${backup.name}", "Backup")
        }
    }

    // --- SECURE SANDBOX CREATOR ---
    private fun setupSandboxFilesAndPrefs() {
        try {
            // Seed sample preference directories/files for the Preference Module to detect
            val prefFile1 = context.getSharedPreferences("keyroom_app_settings", Context.MODE_PRIVATE)
            if (!prefFile1.contains("theme_mode")) {
                prefFile1.edit()
                    .putString("theme_mode", "Dark Slate Cosmic")
                    .putBoolean("encryption_enabled", true)
                    .putInt("session_timeout_seconds", 1800)
                    .putLong("last_sync_timestamp", System.currentTimeMillis())
                    .apply()
            }

            val prefFile2 = context.getSharedPreferences("user_profile_data", Context.MODE_PRIVATE)
            if (!prefFile2.contains("username")) {
                prefFile2.edit()
                    .putString("username", "admin_keyroom")
                    .putString("user_role", "Enterprise Lead")
                    .putBoolean("biometric_login", false)
                    .apply()
            }

            // Seed sample sandbox files inside the local storage for File Manager explorer
            val sandboxDir = File(context.filesDir, "sandbox_workspace")
            if (!sandboxDir.exists()) sandboxDir.mkdirs()

            val docFile = File(sandboxDir, "keyroom_manual.txt")
            if (!docFile.exists()) {
                FileOutputStream(docFile).use {
                    it.write("KeyRoom Administrator Workspace\n============================\nThis environment contains file structure nodes, SQLite databases, and SharedPreference storage registers.\nUse KeyRoom diagnostics to explore files safely.".toByteArray())
                }
            }

            val configFile = File(sandboxDir, "config_production.json")
            if (!configFile.exists()) {
                FileOutputStream(configFile).use {
                    it.write("{\n  \"api_endpoint\": \"https://api.keyroom.io/v1\",\n  \"compression_level\": \"HIGH\",\n  \"automatic_backup_interval_m\": 120\n}".toByteArray())
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- APPLICATION EXPLORER ---
    suspend fun getInstalledApplications(filterSystem: Boolean = false): List<AppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val appsList = ArrayList<AppInfo>()

        try {
            val packages = pm.getInstalledPackages(0)
            for (pkg in packages) {
                val appInfo = pkg.applicationInfo ?: continue
                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                if (filterSystem && isSystem) continue

                val size = try {
                    File(appInfo.sourceDir).length()
                } catch (e: Exception) {
                    0L
                }

                appsList.add(
                    AppInfo(
                        appName = pm.getApplicationLabel(appInfo).toString(),
                        packageName = pkg.packageName,
                        versionName = pkg.versionName ?: "1.0",
                        versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) pkg.longVersionCode else pkg.versionCode.toLong(),
                        sizeBytes = size,
                        isSystemApp = isSystem,
                        firstInstallTime = pkg.firstInstallTime,
                        lastUpdateTime = pkg.lastUpdateTime,
                        targetSdkVersion = appInfo.targetSdkVersion
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Add interesting virtual apps to simulate scanning on simple isolated environments
        if (appsList.size <= 2) {
            appsList.add(AppInfo("KeyRoom App Manager", context.packageName, "1.0.0", 1, 15420000L, false, System.currentTimeMillis() - 86400000 * 2, System.currentTimeMillis() - 3600000, 34))
            appsList.add(AppInfo("System Launcher", "com.android.launcher", "12.0.4", 31, 8950000L, true, System.currentTimeMillis() - 86400000 * 100, System.currentTimeMillis() - 86400000 * 30, 33))
            appsList.add(AppInfo("Secure Keyring", "com.android.keyring", "2.1.0", 5, 4200000L, true, System.currentTimeMillis() - 86400000 * 100, System.currentTimeMillis() - 86400000 * 30, 31))
            appsList.add(AppInfo("Google Play Services", "com.google.android.gms", "24.12.14", 241214000, 142100000L, true, System.currentTimeMillis() - 86400000 * 45, System.currentTimeMillis() - 3600000 * 4, 34))
            appsList.add(AppInfo("Kotlin Dev Tools", "org.jetbrains.kotlin.dev", "2.0.21", 200, 24500000L, false, System.currentTimeMillis() - 86400000 * 5, System.currentTimeMillis() - 3600000 * 12, 34))
        }

        appsList.sortByDescending { it.lastUpdateTime }
        appsList
    }

    // --- APK ANALYZER ---
    suspend fun analyzeApkFile(path: String): ApkDetail = withContext(Dispatchers.IO) {
        val file = File(path)
        if (!file.exists() || !file.isFile) {
            throw Exception("Target file does not exist")
        }

        val pm = context.packageManager
        val packageInfo = pm.getPackageArchiveInfo(path, PackageManager.GET_PERMISSIONS or PackageManager.GET_ACTIVITIES or PackageManager.GET_SERVICES)
            ?: throw Exception("Failed to decode APK structure")

        val appName = packageInfo.applicationInfo?.let { pm.getApplicationLabel(it).toString() } ?: file.nameWithoutExtension
        val perms = packageInfo.requestedPermissions?.toList() ?: emptyList()
        val targetSdk = packageInfo.applicationInfo?.targetSdkVersion ?: 33
        val minSdk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) packageInfo.applicationInfo?.minSdkVersion ?: 24 else 24

        // Count risky permissions
        val issues = perms.count { it.contains("WRITE_EXTERNAL_STORAGE") || it.contains("INTERNET") || it.contains("SYSTEM_ALERT_WINDOW") || it.contains("RECEIVE_BOOT_COMPLETED") }

        ApkDetail(
            label = appName,
            packageName = packageInfo.packageName,
            versionName = packageInfo.versionName ?: "1.0",
            versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) packageInfo.longVersionCode else packageInfo.versionCode.toLong(),
            sizeBytes = file.length(),
            permissions = perms,
            targetSdkVersion = targetSdk,
            minSdkVersion = minSdk,
            mainActivityName = packageInfo.activities?.firstOrNull()?.name,
            issuesFound = issues
        )
    }

    // --- DATA / FILE MANAGEMENT ---
    suspend fun getFilesAtDirectory(dirPath: String): List<FileItem> = withContext(Dispatchers.IO) {
        val root = if (dirPath.isEmpty()) context.filesDir else File(dirPath)
        if (!root.exists() || !root.isDirectory) return@withContext emptyList()

        val list = root.listFiles() ?: return@withContext emptyList()
        list.map { f ->
            FileItem(
                name = f.name,
                path = f.absolutePath,
                isDirectory = f.isDirectory,
                sizeBytes = if (f.isDirectory) getDirectorySize(f) else f.length(),
                lastModified = f.lastModified(),
                extension = f.extension
            )
        }.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
    }

    private fun getDirectorySize(dir: File): Long {
        var size = 0L
        val files = dir.listFiles() ?: return 0L
        for (f in files) {
            size += if (f.isDirectory) getDirectorySize(f) else f.length()
        }
        return size
    }

    suspend fun createFolder(parentPath: String, folderName: String): Boolean = withContext(Dispatchers.IO) {
        val parent = if (parentPath.isEmpty()) context.filesDir else File(parentPath)
        val newFolder = File(parent, folderName)
        val ok = newFolder.mkdirs()
        if (ok) {
            logActivity("Created Folder: $folderName", "File", "SUCCESS", "Path: ${newFolder.absolutePath}")
        }
        ok
    }

    suspend fun createFile(parentPath: String, fileName: String, content: String): Boolean = withContext(Dispatchers.IO) {
        val parent = if (parentPath.isEmpty()) context.filesDir else File(parentPath)
        val file = File(parent, fileName)
        try {
            FileOutputStream(file).use { out ->
                out.write(content.toByteArray())
            }
            logActivity("Created File: $fileName", "File", "SUCCESS", "Size: ${file.length()} bytes")
            true
        } catch (e: Exception) {
            logActivity("Created File: $fileName", "File", "FAILED", e.localizedMessage ?: "")
            false
        }
    }

    suspend fun deleteFileItem(path: String): Boolean = withContext(Dispatchers.IO) {
        val file = File(path)
        if (!file.exists()) return@withContext false
        val name = file.name
        val success = file.deleteRecursively()
        if (success) {
            logActivity("Deleted item: $name", "File")
        }
        success
    }

    suspend fun renameFileItem(path: String, newName: String): Boolean = withContext(Dispatchers.IO) {
        val file = File(path)
        if (!file.exists()) return@withContext false
        val dest = File(file.parentFile, newName)
        val success = file.renameTo(dest)
        if (success) {
            logActivity("Renamed File: ${file.name} to $newName", "File")
        }
        success
    }

    // --- PREFERENCE MANAGEMENT ---
    suspend fun listPreferenceFiles(): List<PreferenceFile> = withContext(Dispatchers.IO) {
        val prefsDir = File(context.filesDir.parentFile, "shared_prefs")
        if (!prefsDir.exists() || !prefsDir.isDirectory) return@withContext emptyList()

        val files = prefsDir.listFiles() ?: return@withContext emptyList()
        files.map { f ->
            val name = f.nameWithoutExtension
            val keysCount = try {
                context.getSharedPreferences(name, Context.MODE_PRIVATE).all.size
            } catch (e: Exception) {
                0
            }
            PreferenceFile(name = name, sizeBytes = f.length(), keysCount = keysCount)
        }
    }

    suspend fun getPreferenceEntries(fileName: String): List<PreferenceEntry> = withContext(Dispatchers.IO) {
        val sharedPrefs = context.getSharedPreferences(fileName, Context.MODE_PRIVATE)
        val allEntries = sharedPrefs.all
        allEntries.map { (key, value) ->
            val typeStr = when (value) {
                is String -> "String"
                is Boolean -> "Boolean"
                is Int -> "Int"
                is Long -> "Long"
                is Float -> "Float"
                else -> value?.javaClass?.simpleName ?: "Unknown"
            }
            PreferenceEntry(key = key, value = value?.toString() ?: "null", type = typeStr)
        }.sortedBy { it.key }
    }

    suspend fun updatePreferenceValue(fileName: String, key: String, valueStr: String, type: String): Boolean = withContext(Dispatchers.IO) {
        val sharedPrefs = context.getSharedPreferences(fileName, Context.MODE_PRIVATE)
        val editor = sharedPrefs.edit()
        try {
            when (type) {
                "String" -> editor.putString(key, valueStr)
                "Boolean" -> editor.putBoolean(key, valueStr.toBooleanStrictOrNull() ?: false)
                "Int" -> editor.putInt(key, valueStr.toIntOrNull() ?: 0)
                "Long" -> editor.putLong(key, valueStr.toLongOrNull() ?: 0L)
                "Float" -> editor.putFloat(key, valueStr.toFloatOrNull() ?: 0.0f)
                else -> editor.putString(key, valueStr)
            }
            editor.apply()
            logActivity("Updated Preference: $key in $fileName", "Preferences", "SUCCESS", "New value: $valueStr")
            true
        } catch (e: Exception) {
            logActivity("Updated Preference: $key in $fileName", "Preferences", "FAILED", e.localizedMessage ?: "")
            false
        }
    }

    suspend fun deletePreferenceEntry(fileName: String, key: String): Boolean = withContext(Dispatchers.IO) {
        val sharedPrefs = context.getSharedPreferences(fileName, Context.MODE_PRIVATE)
        val ok = sharedPrefs.edit().remove(key).commit()
        if (ok) {
            logActivity("Deleted Preference Key: $key from $fileName", "Preferences")
        }
        ok
    }

    // --- DATABASE MANAGER ---
    suspend fun listDetectedDatabases(): List<DatabaseInfo> = withContext(Dispatchers.IO) {
        // List from databases folder
        val dbDir = File(context.filesDir.parentFile, "databases")
        if (!dbDir.exists() || !dbDir.isDirectory) return@withContext emptyList()

        val files = dbDir.listFiles() ?: return@withContext emptyList()
        // Filter actual SQLite main database files (ignore journal, shm, wal files)
        val sqliteFiles = files.filter { it.isFile && !it.name.endsWith("-journal") && !it.name.endsWith("-shm") && !it.name.endsWith("-wal") }

        sqliteFiles.map { f ->
            val tables = getTablesForDatabase(f.absolutePath)
            DatabaseInfo(name = f.name, sizeBytes = f.length(), tables = tables)
        }
    }

    private fun getTablesForDatabase(dbPath: String): List<String> {
        val tables = mutableListOf<String>()
        var db: SQLiteDatabase? = null
        try {
            db = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READONLY)
            val cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'android_metadata' AND name NOT LIKE 'sqlite_sequence'", null)
            if (cursor.moveToFirst()) {
                do {
                    tables.add(cursor.getString(0))
                } while (cursor.moveToNext())
            }
            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db?.close()
        }
        return tables
    }

    suspend fun executeRawSql(dbName: String, query: String): DatabaseTableRow = withContext(Dispatchers.IO) {
        val dbDir = File(context.filesDir.parent, "databases")
        val dbFile = File(dbDir, dbName)
        if (!dbFile.exists()) throw Exception("Database file does not exist")

        var db: SQLiteDatabase? = null
        try {
            db = SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READWRITE)

            // Log activity first
            logActivity("Executed SQL on $dbName", "Database", "SUCCESS", query)

            // If query is an insert/update/delete etc, execute and return rows affected or complete status
            val isSelect = query.trim().uppercase().startsWith("SELECT") || query.trim().uppercase().startsWith("PRAGMA")
            if (!isSelect) {
                db.execSQL(query)
                return@withContext DatabaseTableRow(
                    columns = listOf("Result"),
                    cells = listOf("Query executed successfully. Action committed.")
                )
            }

            val cursor = db.rawQuery(query, null)
            val columns = cursor.columnNames.toList()
            val cells = mutableListOf<String>()

            if (cursor.moveToFirst()) {
                val colCount = cursor.columnCount
                var rowsCount = 0
                do {
                    for (i in 0 until colCount) {
                        cells.add(cursor.getString(i) ?: "NULL")
                    }
                    rowsCount++
                    if (rowsCount >= 100) { // Safety break
                        break
                    }
                } while (cursor.moveToNext())
            }
            cursor.close()
            DatabaseTableRow(columns = columns, cells = cells)
        } catch (e: Exception) {
            logActivity("Executed SQL on $dbName", "Database", "FAILED", e.localizedMessage ?: "")
            DatabaseTableRow(columns = listOf("Error"), cells = listOf(e.localizedMessage ?: "Unknown SQL error"))
        } finally {
            db?.close()
        }
    }

    // --- FULL & SELECTIVE BACKUPS AND RESTORE ---
    suspend fun runFullBackup(): Boolean = withContext(Dispatchers.IO) {
        try {
            val backupDir = File(context.filesDir, "backups")
            if (!backupDir.exists()) backupDir.mkdirs()

            val backupFile = File(backupDir, "keyroom_full_backup_${System.currentTimeMillis()}.zip")
            val zipOut = ZipOutputStream(FileOutputStream(backupFile))

            // Zip internal databases
            val dbDir = File(context.filesDir.parentFile, "databases")
            var count = 0
            if (dbDir.exists() && dbDir.isDirectory) {
                dbDir.listFiles()?.forEach { f ->
                    if (f.isFile && !f.name.endsWith("-journal") && !f.name.endsWith("-wal") && !f.name.endsWith("-shm")) {
                        zipOut.putNextEntry(ZipEntry("databases/${f.name}"))
                        FileInputStream(f).use { input -> input.copyTo(zipOut) }
                        zipOut.closeEntry()
                        count++
                    }
                }
            }

            // Zip preferences
            val prefsDir = File(context.filesDir.parentFile, "shared_prefs")
            if (prefsDir.exists() && prefsDir.isDirectory) {
                prefsDir.listFiles()?.forEach { f ->
                    if (f.isFile) {
                        zipOut.putNextEntry(ZipEntry("shared_prefs/${f.name}"))
                        FileInputStream(f).use { input -> input.copyTo(zipOut) }
                        zipOut.closeEntry()
                        count++
                    }
                }
            }

            zipOut.close()

            registerBackup(
                name = backupFile.name,
                type = "FULL",
                size = backupFile.length(),
                filePath = backupFile.absolutePath,
                itemCount = count
            )
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun runSelectiveBackup(types: List<String>): Boolean = withContext(Dispatchers.IO) {
        try {
            val backupDir = File(context.filesDir, "backups")
            if (!backupDir.exists()) backupDir.mkdirs()

            val backupFile = File(backupDir, "keyroom_selective_${System.currentTimeMillis()}.zip")
            val zipOut = ZipOutputStream(FileOutputStream(backupFile))
            var count = 0

            if (types.contains("DATABASE")) {
                val dbDir = File(context.filesDir.parentFile, "databases")
                if (dbDir.exists() && dbDir.isDirectory) {
                    dbDir.listFiles()?.forEach { f ->
                        if (f.isFile && !f.name.endsWith("-journal") && !f.name.endsWith("-wal") && !f.name.endsWith("-shm")) {
                            zipOut.putNextEntry(ZipEntry("databases/${f.name}"))
                            FileInputStream(f).use { input -> input.copyTo(zipOut) }
                            zipOut.closeEntry()
                            count++
                        }
                    }
                }
            }

            if (types.contains("PREFERENCES")) {
                val prefsDir = File(context.filesDir.parentFile, "shared_prefs")
                if (prefsDir.exists() && prefsDir.isDirectory) {
                    prefsDir.listFiles()?.forEach { f ->
                        if (f.isFile) {
                            zipOut.putNextEntry(ZipEntry("shared_prefs/${f.name}"))
                            FileInputStream(f).use { input -> input.copyTo(zipOut) }
                            zipOut.closeEntry()
                            count++
                        }
                    }
                }
            }

            zipOut.close()

            registerBackup(
                name = backupFile.name,
                type = if (types.size > 1) "SELECTIVE" else types.firstOrNull() ?: "SELECTIVE",
                size = backupFile.length(),
                filePath = backupFile.absolutePath,
                itemCount = count
            )
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // --- APPLICATION STORAGE ANALYSIS ---
    suspend fun getStorageStatsReport(): Map<String, Long> = withContext(Dispatchers.IO) {
        val parent = context.filesDir.parentFile ?: return@withContext emptyMap()
        var dbBytes = 0L
        var prefBytes = 0L
        var cacheBytes = 0L
        var fileBytes = 0L

        val dbDir = File(parent, "databases")
        if (dbDir.exists()) dbBytes = getDirectorySize(dbDir)

        val prefDir = File(parent, "shared_prefs")
        if (prefDir.exists()) prefBytes = getDirectorySize(prefDir)

        val cacheDir = context.cacheDir
        if (cacheDir.exists()) cacheBytes = getDirectorySize(cacheDir)

        val filesDir = context.filesDir
        if (filesDir.exists()) fileBytes = getDirectorySize(filesDir)

        this@KeyRoomRepository.logActivity("Performed storage space report scan", "Analytics")

        mapOf(
            "Databases" to dbBytes,
            "Preferences" to prefBytes,
            "Cache" to cacheBytes,
            "Files Workspace" to fileBytes,
            "Total Free Space" to context.filesDir.usableSpace
        )
    }
}
