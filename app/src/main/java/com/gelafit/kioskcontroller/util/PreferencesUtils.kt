package com.gelafit.kioskcontroller.util

import android.content.Context
import android.content.SharedPreferences

object PreferencesUtils {
    private const val PREFS_NAME = "kiosk_controller_prefs"
    private const val KEY_SELECTED_APP_PACKAGE = "selected_app_package"
    private const val KEY_SELECTED_APP_NAME = "selected_app_name"
    private const val KEY_AUTO_START_ON_BOOT = "auto_start_on_boot"
    private const val KEY_SERVICE_ENABLED = "service_enabled"
    
    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * Salva o app selecionado para modo kiosk
     */
    fun saveSelectedApp(context: Context, packageName: String, appName: String) {
        getSharedPreferences(context).edit()
            .putString(KEY_SELECTED_APP_PACKAGE, packageName)
            .putString(KEY_SELECTED_APP_NAME, appName)
            .apply()
    }
    
    /**
     * Obtém o package name do app selecionado
     */
    fun getSelectedAppPackage(context: Context): String? {
        return getSharedPreferences(context).getString(KEY_SELECTED_APP_PACKAGE, null)
    }
    
    /**
     * Obtém o nome do app selecionado
     */
    fun getSelectedAppName(context: Context): String? {
        return getSharedPreferences(context).getString(KEY_SELECTED_APP_NAME, null)
    }
    
    /**
     * Verifica se há um app selecionado
     */
    fun hasSelectedApp(context: Context): Boolean {
        return getSelectedAppPackage(context) != null
    }
    
    /**
     * Remove o app selecionado
     */
    fun clearSelectedApp(context: Context) {
        getSharedPreferences(context).edit()
            .remove(KEY_SELECTED_APP_PACKAGE)
            .remove(KEY_SELECTED_APP_NAME)
            .apply()
    }
    
    /**
     * Define se o serviço deve iniciar automaticamente no boot
     */
    fun setAutoStartOnBoot(context: Context, enabled: Boolean) {
        getSharedPreferences(context).edit()
            .putBoolean(KEY_AUTO_START_ON_BOOT, enabled)
            .apply()
    }
    
    /**
     * Verifica se o serviço deve iniciar automaticamente no boot
     */
    fun shouldAutoStartOnBoot(context: Context): Boolean {
        return getSharedPreferences(context).getBoolean(KEY_AUTO_START_ON_BOOT, true)
    }
    
    /**
     * Salva o estado do serviço (se está habilitado)
     */
    fun setServiceEnabled(context: Context, enabled: Boolean) {
        getSharedPreferences(context).edit()
            .putBoolean(KEY_SERVICE_ENABLED, enabled)
            .apply()
    }
    
    /**
     * Verifica se o serviço está habilitado
     */
    fun isServiceEnabled(context: Context): Boolean {
        return getSharedPreferences(context).getBoolean(KEY_SERVICE_ENABLED, false)
    }
}
