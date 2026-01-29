package com.gelafit.kioskcontroller.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.gelafit.kioskcontroller.service.KioskMonitorService
import com.gelafit.kioskcontroller.util.PreferencesUtils

class BootReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "BootReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || 
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            intent.action == Intent.ACTION_PACKAGE_REPLACED) {
            
            Log.d(TAG, "Dispositivo reiniciado ou app atualizado")
            
            // Verifica se deve iniciar automaticamente no boot
            if (!PreferencesUtils.shouldAutoStartOnBoot(context)) {
                Log.d(TAG, "Auto-start no boot está desabilitado")
                return
            }
            
            // Verifica se há um app selecionado antes de iniciar
            if (PreferencesUtils.hasSelectedApp(context)) {
                Log.d(TAG, "Iniciando serviço de monitoramento automaticamente")
                startKioskService(context)
            } else {
                Log.d(TAG, "Nenhum app selecionado, não iniciando serviço")
            }
        }
    }
    
    private fun startKioskService(context: Context) {
        val serviceIntent = Intent(context, KioskMonitorService::class.java)
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
        
        // Inicia também o WorkManager como backup
        com.gelafit.kioskcontroller.service.KioskMonitorWorker.startPeriodicWork(context)
    }
}
