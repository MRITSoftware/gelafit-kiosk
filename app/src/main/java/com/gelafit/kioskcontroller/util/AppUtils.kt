package com.gelafit.kioskcontroller.util

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable

data class InstalledApp(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val isSystemApp: Boolean
)

object AppUtils {
    
    /**
     * Lista todos os apps instalados no dispositivo
     */
    fun getInstalledApps(context: Context): List<InstalledApp> {
        val packageManager = context.packageManager
        val installedApps = mutableListOf<InstalledApp>()
        
        val packages = packageManager.getInstalledPackages(PackageManager.GET_META_DATA)
        
        for (packageInfo in packages) {
            try {
                val appName = packageManager.getApplicationLabel(packageInfo.applicationInfo).toString()
                val icon = packageManager.getApplicationIcon(packageInfo.applicationInfo)
                val isSystemApp = (packageInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                
                installedApps.add(
                    InstalledApp(
                        packageName = packageInfo.packageName,
                        appName = appName,
                        icon = icon,
                        isSystemApp = isSystemApp
                    )
                )
            } catch (e: Exception) {
                // Ignora apps que não podem ser carregados
            }
        }
        
        // Ordena por nome
        return installedApps.sortedBy { it.appName }
    }
    
    /**
     * Lista apenas apps do usuário (não sistema)
     */
    fun getUserApps(context: Context): List<InstalledApp> {
        return getInstalledApps(context).filter { !it.isSystemApp }
    }
    
    /**
     * Verifica se um app está instalado
     */
    fun isAppInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
    
    /**
     * Verifica se um app está rodando
     */
    fun isAppRunning(context: Context, packageName: String): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val runningApps = activityManager.getRunningAppProcesses()
        
        return runningApps?.any { it.processName == packageName } == true
    }
    
    /**
     * Abre um app pelo package name
     */
    fun launchApp(context: Context, packageName: String) {
        try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Traz um app para o foreground
     */
    fun bringAppToForeground(context: Context, packageName: String) {
        try {
            val intent = Intent().apply {
                action = Intent.ACTION_MAIN
                addCategory(Intent.CATEGORY_LAUNCHER)
                setPackage(packageName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                        Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
