package com.gelafit.kioskcontroller.util

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import com.gelafit.kioskcontroller.SupabaseConfig

object DeviceUtils {
    /**
     * Obtém o Device ID único do Android
     */
    fun getDeviceId(context: Context): String {
        return Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "unknown-device"
    }
    
    /**
     * Verifica se o app Flutter está rodando
     */
    fun isFlutterAppRunning(context: Context): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningApps = activityManager.getRunningAppProcesses()
        
        return runningApps?.any { it.processName == SupabaseConfig.FLUTTER_APP_PACKAGE } == true
    }
    
    /**
     * Abre o app Flutter
     */
    fun launchFlutterApp(context: Context) {
        try {
            val intent = context.packageManager.getLaunchIntentForPackage(
                SupabaseConfig.FLUTTER_APP_PACKAGE
            )
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
     * Força o app Flutter para o foreground
     */
    fun bringFlutterAppToForeground(context: Context) {
        try {
            val intent = Intent().apply {
                action = Intent.ACTION_MAIN
                addCategory(Intent.CATEGORY_LAUNCHER)
                setPackage(SupabaseConfig.FLUTTER_APP_PACKAGE)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                        Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Verifica se o app Flutter está instalado
     */
    fun isFlutterAppInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(
                SupabaseConfig.FLUTTER_APP_PACKAGE,
                PackageManager.GET_ACTIVITIES
            )
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
    
    /**
     * Verifica se o app está em primeiro plano
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    fun isAppInForeground(context: Context, packageName: String): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningTasks = activityManager.getRunningTasks(1)
        
        return if (runningTasks.isNotEmpty()) {
            runningTasks[0].topActivity?.packageName == packageName
        } else {
            false
        }
    }
}
