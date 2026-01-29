package com.gelafit.kioskcontroller.util

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi

data class PermissionStatus(
    val name: String,
    val description: String,
    val isGranted: Boolean,
    val settingsIntent: Intent? = null,
    val required: Boolean = true
)

object PermissionUtils {
    
    /**
     * Verifica todas as permissões necessárias e retorna o status
     */
    fun checkAllPermissions(context: Context): List<PermissionStatus> {
        val permissions = mutableListOf<PermissionStatus>()
        
        // 1. Permissão de Apps sobrepostos (Overlay)
        permissions.add(checkOverlayPermission(context))
        
        // 2. Permissão de Uso de acesso (Usage Access)
        permissions.add(checkUsageStatsPermission(context))
        
        return permissions
    }
    
    /**
     * Verifica se a permissão de Apps sobrepostos está concedida
     */
    private fun checkOverlayPermission(context: Context): PermissionStatus {
        val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
        
        val intent = if (!granted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
        } else {
            null
        }
        
        return PermissionStatus(
            name = "Apps sobrepostos",
            description = "Permite que o app traga outros apps para o primeiro plano",
            isGranted = granted,
            settingsIntent = intent,
            required = true
        )
    }
    
    /**
     * Verifica se a permissão de Uso de acesso está concedida
     */
    private fun checkUsageStatsPermission(context: Context): PermissionStatus {
        val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
            mode == AppOpsManager.MODE_ALLOWED
        } else {
            true
        }
        
        val intent = if (!granted) {
            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        } else {
            null
        }
        
        return PermissionStatus(
            name = "Uso de acesso",
            description = "Permite que o app verifique quais apps estão em execução",
            isGranted = granted,
            settingsIntent = intent,
            required = true
        )
    }
    
    /**
     * Verifica se todas as permissões necessárias estão concedidas
     */
    fun areAllPermissionsGranted(context: Context): Boolean {
        return checkAllPermissions(context).all { it.isGranted }
    }
    
    /**
     * Abre as configurações para uma permissão específica
     */
    fun openPermissionSettings(context: Context, permission: PermissionStatus) {
        permission.settingsIntent?.let {
            context.startActivity(it)
        }
    }
}
