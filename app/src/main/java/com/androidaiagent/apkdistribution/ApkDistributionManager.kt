package com.androidaiagent.apkdistribution

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class ApkDistributionManager(
    private val context: Context
) {
    private val _serverRunning = MutableStateFlow(false)
    val serverRunning: StateFlow<Boolean> = _serverRunning.asStateFlow()
    
    private val _downloadUrl = MutableStateFlow<String?>(null)
    val downloadUrl: StateFlow<String?> = _downloadUrl.asStateFlow()
    
    private val _lanIp = MutableStateFlow<String?>(null)
    val lanIp: StateFlow<String?> = _lanIp.asStateFlow()
    
    private val _currentApkVersion = MutableStateFlow<String?>(null)
    val currentApkVersion: StateFlow<String?> = _currentApkVersion.asStateFlow()
    
    private val _qrCodeData = MutableStateFlow<String?>(null)
    val qrCodeData: StateFlow<String?> = _qrCodeData.asStateFlow()
    
    private var pythonServerProcess: Process? = null
    
    fun startServer(apkPath: String) {
        if (_serverRunning.value) return
        
        val apkFile = File(apkPath)
        if (!apkFile.exists()) {
            return
        }
        
        _currentApkVersion.value = extractVersion(apkFile.name)
        
        try {
            val pythonScript = findPythonScript()
            if (pythonScript != null) {
                val processBuilder = ProcessBuilder(
                    "python3",
                    pythonScript,
                    "--apk-path", apkPath.absolutePath,
                    "--port", "8080"
                )
                
                pythonServerProcess = processBuilder.start()
                _serverRunning.value = true
                
                detectNetworkInfo()
                generateDownloadUrl()
            }
        } catch (e: Exception) {
            _serverRunning.value = false
        }
    }
    
    fun stopServer() {
        pythonServerProcess?.destroy()
        pythonServerProcess = null
        _serverRunning.value = false
        _downloadUrl.value = null
        _lanIp.value = null
        _qrCodeData.value = null
    }
    
    private fun findPythonScript(): String? {
        val possiblePaths = listOf(
            "${context.filesDir.absolutePath}/../apk_distribution/local_server/server.py",
            "${context.getExternalFilesDir(null)?.absolutePath}/apk_distribution/local_server/server.py"
        )
        
        for (path in possiblePaths) {
            if (File(path).exists()) {
                return path
            }
        }
        return null
    }
    
    private fun detectNetworkInfo() {
        try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            for (networkInterface in interfaces) {
                if (!networkInterface.isLoopback && networkInterface.isUp) {
                    val addresses = networkInterface.inetAddresses
                    for (address in addresses) {
                        if (!address.isLinkLocalAddress && address.hostAddress?.contains(".") == true) {
                            _lanIp.value = address.hostAddress
                            return
                        }
                    }
                }
            }
        } catch (e: Exception) {
            _lanIp.value = "127.0.0.1"
        }
    }
    
    private fun generateDownloadUrl() {
        val ip = _lanIp.value ?: return
        _downloadUrl.value = "http://$ip:8080/download/app.apk"
        _qrCodeData.value = _downloadUrl.value
    }
    
    private fun extractVersion(filename: String): String {
        val versionPattern = Regex("""(\d+\.\d+\.\d+)""")
        val match = versionPattern.find(filename)
        return match?.value ?: "unknown"
    }
    
    fun refreshQrCode() {
        _qrCodeData.value = _downloadUrl.value
    }
    
    fun getServerStatus(): ServerStatus {
        return if (_serverRunning.value) {
            ServerStatus.RUNNING
        } else {
            ServerStatus.STOPPED
        }
    }
}

enum class ServerStatus {
    RUNNING,
    STOPPED,
    ERROR
}
