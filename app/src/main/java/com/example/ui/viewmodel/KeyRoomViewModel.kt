package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.db.*
import com.example.data.model.*
import com.example.data.repository.KeyRoomRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class KeyRoomViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = KeyRoomRepository(application)

    // --- SCREEN NAVIGATION ---
    private val _currentRoute = MutableStateFlow("dashboard")
    val currentRoute: StateFlow<String> = _currentRoute.asStateFlow()

    fun navigateTo(route: String) {
        _currentRoute.value = route
        viewModelScope.launch {
            repository.logActivity("Navigated to destination: $route", "Navigation")
        }
    }

    // --- DATABASE ROOM READS ---
    val recentActivities: StateFlow<List<RecentActivity>> = repository.recentActivities
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val backups: StateFlow<List<BackupRecord>> = repository.backupRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedQueries: StateFlow<List<SavedQuery>> = repository.savedQueries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- INSTALLED APPLICATIONS ---
    private val _allApps = MutableStateFlow<List<AppInfo>>(emptyList())
    private val _appsSearchQuery = MutableStateFlow("")
    val appsSearchQuery = _appsSearchQuery.asStateFlow()
    private val _filterSystem = MutableStateFlow(true) // Default hides system apps
    val filterSystem = _filterSystem.asStateFlow()

    val filteredApps: StateFlow<List<AppInfo>> = combine(_allApps, _appsSearchQuery, _filterSystem) { apps, query, hideSys ->
        apps.filter {
            (it.appName.contains(query, ignoreCase = true) || it.packageName.contains(query, ignoreCase = true)) &&
            (!hideSys || !it.isSystemApp)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateAppsSearch(query: String) {
        _appsSearchQuery.value = query
    }

    fun toggleSystemAppsFilter() {
        _filterSystem.value = !_filterSystem.value
    }

    fun scanInstalledApps() {
        viewModelScope.launch {
            _allApps.value = repository.getInstalledApplications(false)
            repository.logActivity("Scanned system and user installed applications", "Explorer", "SUCCESS", "Found ${_allApps.value.size} packages.")
        }
    }

    // --- PREFERENCES MANAGER ---
    private val _preferenceFiles = MutableStateFlow<List<PreferenceFile>>(emptyList())
    val preferenceFiles = _preferenceFiles.asStateFlow()

    private val _selectedPrefFile = MutableStateFlow<String?>(null)
    val selectedPrefFile = _selectedPrefFile.asStateFlow()

    private val _prefEntries = MutableStateFlow<List<PreferenceEntry>>(emptyList())
    private val _prefSearch = MutableStateFlow("")
    val prefSearch = _prefSearch.asStateFlow()

    val filteredPrefEntries: StateFlow<List<PreferenceEntry>> = combine(_prefEntries, _prefSearch) { entries, query ->
        if (query.isEmpty()) entries else entries.filter { it.key.contains(query, ignoreCase = true) || it.value.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun refreshPreferenceFiles() {
        viewModelScope.launch {
            _preferenceFiles.value = repository.listPreferenceFiles()
        }
    }

    fun selectPreferenceFile(name: String?) {
        _selectedPrefFile.value = name
        _prefSearch.value = ""
        if (name != null) {
            loadPreferenceEntries(name)
        } else {
            _prefEntries.value = emptyList()
        }
    }

    fun updatePrefSearch(q: String) {
        _prefSearch.value = q
    }

    fun loadPreferenceEntries(name: String) {
        viewModelScope.launch {
            _prefEntries.value = repository.getPreferenceEntries(name)
        }
    }

    fun updatePrefValue(fileName: String, key: String, valueStr: String, type: String) {
        viewModelScope.launch {
            val ok = repository.updatePreferenceValue(fileName, key, valueStr, type)
            if (ok) {
                loadPreferenceEntries(fileName)
                refreshPreferenceFiles()
            }
        }
    }

    fun deletePrefKey(fileName: String, key: String) {
        viewModelScope.launch {
            val ok = repository.deletePreferenceEntry(fileName, key)
            if (ok) {
                loadPreferenceEntries(fileName)
                refreshPreferenceFiles()
            }
        }
    }

    // --- DATABASE EXPLORER ---
    private val _detectedDatabases = MutableStateFlow<List<DatabaseInfo>>(emptyList())
    val detectedDatabases = _detectedDatabases.asStateFlow()

    private val _selectedDatabase = MutableStateFlow<DatabaseInfo?>(null)
    val selectedDatabase = _selectedDatabase.asStateFlow()

    private val _selectedTable = MutableStateFlow<String?>(null)
    val selectedTable = _selectedTable.asStateFlow()

    private val _sqlOutput = MutableStateFlow<DatabaseTableRow?>(null)
    val sqlOutput = _sqlOutput.asStateFlow()

    private val _sqlQueryText = MutableStateFlow("SELECT * FROM sqlite_master LIMIT 10;")
    val sqlQueryText = _sqlQueryText.asStateFlow()

    fun updateSqlQuery(q: String) {
        _sqlQueryText.value = q
    }

    fun refreshDatabases() {
        viewModelScope.launch {
            _detectedDatabases.value = repository.listDetectedDatabases()
        }
    }

    fun selectDatabase(dbInfo: DatabaseInfo?) {
        _selectedDatabase.value = dbInfo
        _selectedTable.value = null
        _sqlOutput.value = null
        if (dbInfo != null) {
            _sqlQueryText.value = "SELECT * FROM sqlite_master LIMIT 10;"
        }
    }

    fun selectDatabaseTable(tableName: String) {
        _selectedTable.value = tableName
        _sqlQueryText.value = "SELECT * FROM $tableName LIMIT 50;"
        executeCurrentSql()
    }

    fun executeCurrentSql() {
        val db = _selectedDatabase.value ?: return
        viewModelScope.launch {
            try {
                _sqlOutput.value = repository.executeRawSql(db.name, _sqlQueryText.value)
            } catch (e: Exception) {
                _sqlOutput.value = DatabaseTableRow(listOf("Error"), listOf(e.localizedMessage ?: "SQL execution crash"))
            }
        }
    }

    fun saveCurrentSql(title: String) {
        viewModelScope.launch {
            repository.saveQuery(title, _sqlQueryText.value)
        }
    }

    // --- STORAGE ANALYTICS ---
    private val _storageStats = MutableStateFlow<Map<String, Long>>(emptyMap())
    val storageStats = _storageStats.asStateFlow()

    fun loadStorageStats() {
        viewModelScope.launch {
            _storageStats.value = repository.getStorageStatsReport()
        }
    }

    // --- BACKUP MANAGEMENT FLOWS ---
    fun backupFullSession() {
        viewModelScope.launch {
            val ok = repository.runFullBackup()
            if (ok) {
                loadStorageStats()
            }
        }
    }

    fun backupSelected(types: List<String>) {
        viewModelScope.launch {
            val ok = repository.runSelectiveBackup(types)
            if (ok) {
                loadStorageStats()
            }
        }
    }

    fun deleteBackup(record: BackupRecord) {
        viewModelScope.launch {
            repository.deleteBackupRecord(record)
            loadStorageStats()
        }
    }

    // --- LOCAL FILES EXPLORER ---
    private val _currentDirectoryPath = MutableStateFlow("")
    val currentDirectoryPath = _currentDirectoryPath.asStateFlow()

    private val _currentDirFiles = MutableStateFlow<List<FileItem>>(emptyList())
    val currentDirFiles = _currentDirFiles.asStateFlow()

    fun loadWorkspaceDirectory(path: String) {
        _currentDirectoryPath.value = path
        viewModelScope.launch {
            _currentDirFiles.value = repository.getFilesAtDirectory(path)
        }
    }

    fun loadWorkspaceRoot() {
        loadWorkspaceDirectory("")
    }

    fun createWorkspaceFolder(folderName: String) {
        viewModelScope.launch {
            val ok = repository.createFolder(_currentDirectoryPath.value, folderName)
            if (ok) {
                loadWorkspaceDirectory(_currentDirectoryPath.value)
            }
        }
    }

    fun createWorkspaceFile(name: String, content: String) {
        viewModelScope.launch {
            val ok = repository.createFile(_currentDirectoryPath.value, name, content)
            if (ok) {
                loadWorkspaceDirectory(_currentDirectoryPath.value)
            }
        }
    }

    fun deleteWorkspaceItem(path: String) {
        viewModelScope.launch {
            val ok = repository.deleteFileItem(path)
            if (ok) {
                loadWorkspaceDirectory(_currentDirectoryPath.value)
            }
        }
    }

    fun renameWorkspaceItem(path: String, newName: String) {
        viewModelScope.launch {
            val ok = repository.renameFileItem(path, newName)
            if (ok) {
                loadWorkspaceDirectory(_currentDirectoryPath.value)
            }
        }
    }

    // --- APK ANALYZER ---
    private val _apkAnalysis = MutableStateFlow<ApkDetail?>(null)
    val apkAnalysis = _apkAnalysis.asStateFlow()

    private val _apkAnalysisError = MutableStateFlow<String?>(null)
    val apkAnalysisError = _apkAnalysisError.asStateFlow()

    fun analyzeLocalApkFile(path: String) {
        _apkAnalysisError.value = null
        _apkAnalysis.value = null
        viewModelScope.launch {
            try {
                _apkAnalysis.value = repository.analyzeApkFile(path)
                repository.logActivity("Analyzed external APK", "APK Analyzer", "SUCCESS", "Target Path: $path")
            } catch (e: Exception) {
                _apkAnalysisError.value = e.localizedMessage ?: "Failed to read binary stream"
                repository.logActivity("Analyzed external APK", "APK Analyzer", "FAILED", e.localizedMessage ?: "")
            }
        }
    }

    // --- VIEW SETTINGS CONFIGURATIONS ---
    private val _themeAccent = MutableStateFlow("Dark Cosmic Slate")
    val themeAccent = _themeAccent.asStateFlow()

    private val _securityPin = MutableStateFlow("")
    val securityPin = _securityPin.asStateFlow()

    private val _biometricAuth = MutableStateFlow(false)
    val biometricAuth = _biometricAuth.asStateFlow()

    init {
        // Read initial state
        scanInstalledApps()
        refreshPreferenceFiles()
        refreshDatabases()
        loadWorkspaceRoot()
        loadStorageStats()
    }

    fun changeThemeAccent(theme: String) {
        _themeAccent.value = theme
        viewModelScope.launch {
            repository.logActivity("Changed dynamic theme accent to $theme", "Settings")
        }
    }

    fun setSecurityPin(pin: String) {
        _securityPin.value = pin
        viewModelScope.launch {
            repository.logActivity("Configured master security PIN locks", "Security")
        }
    }

    fun toggleBiometrics() {
        _biometricAuth.value = !_biometricAuth.value
        viewModelScope.launch {
            repository.logActivity("Toggled biometric authorization to ${_biometricAuth.value}", "Security")
        }
    }

    fun clearLogActivities() {
        viewModelScope.launch {
            repository.clearActivities()
        }
    }
}
