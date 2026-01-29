package com.gelafit.kioskcontroller.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.gelafit.kioskcontroller.MainActivity
import com.gelafit.kioskcontroller.R
import com.gelafit.kioskcontroller.SupabaseConfig
import com.gelafit.kioskcontroller.util.AppUtils
import com.gelafit.kioskcontroller.util.DeviceUtils
import com.gelafit.kioskcontroller.util.PreferencesUtils
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit

class KioskMonitorService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var wakeLock: PowerManager.WakeLock? = null
    private var monitoringJob: Job? = null
    
    companion object {
        private const val TAG = "KioskMonitorService"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "kiosk_monitor_channel"
        private const val CHECK_INTERVAL_SECONDS = 5L // Verifica a cada 5 segundos
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        acquireWakeLock()
        startMonitoring()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        return START_STICKY // Reinicia automaticamente se for morto
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        monitoringJob?.cancel()
        releaseWakeLock()
        serviceScope.cancel()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Kiosk Monitor",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitora o modo kiosk do aplicativo"
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val selectedAppName = PreferencesUtils.getSelectedAppName(this) ?: "Nenhum app selecionado"
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Gelafit Kiosk Controller")
            .setContentText("Monitorando: $selectedAppName")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
    
    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "GelafitKiosk::WakeLock"
        ).apply {
            acquire(10 * 60 * 60 * 1000L) // 10 horas
        }
    }
    
    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
    }
    
    private var lastKioskModeState: Boolean? = null
    
    private fun startMonitoring() {
        monitoringJob = serviceScope.launch {
            val deviceId = DeviceUtils.getDeviceId(this@KioskMonitorService)
            
            // Registra o dispositivo na primeira execução
            SupabaseConfig.registerDevice(deviceId)
            
            while (isActive) {
                try {
                    // Busca o status do kiosk_mode
                    val kioskMode = SupabaseConfig.getKioskModeStatus(deviceId)
                    
                    Log.d(TAG, "Kiosk Mode Status: $kioskMode (anterior: $lastKioskModeState)")
                    
                    // Detecta mudança de estado: se mudou de false/null para true, abre o app
                    val stateChanged = lastKioskModeState != kioskMode
                    val justActivated = kioskMode == true && lastKioskModeState != true
                    
                    if (kioskMode == true) {
                        // Modo kiosk ativo - mantém o app fixo
                        if (justActivated) {
                            Log.d(TAG, "Kiosk mode ATIVADO - abrindo app selecionado")
                            // Força abertura do app quando ativado
                            enforceKioskMode(forceOpen = true)
                        } else {
                            enforceKioskMode(forceOpen = false)
                        }
                    } else {
                        // Modo kiosk inativo - permite uso normal
                        if (stateChanged && lastKioskModeState == true) {
                            Log.d(TAG, "Kiosk mode DESATIVADO")
                        }
                    }
                    
                    lastKioskModeState = kioskMode
                    delay(CHECK_INTERVAL_SECONDS * 1000)
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao monitorar kiosk mode", e)
                    delay(CHECK_INTERVAL_SECONDS * 1000)
                }
            }
        }
    }
    
    private suspend fun enforceKioskMode(forceOpen: Boolean = false) {
        withContext(Dispatchers.Main) {
            val selectedAppPackage = PreferencesUtils.getSelectedAppPackage(this@KioskMonitorService)
            
            if (selectedAppPackage == null) {
                Log.w(TAG, "Nenhum app selecionado para modo kiosk")
                return@withContext
            }
            
            // Verifica se o app selecionado está instalado
            if (!AppUtils.isAppInstalled(this@KioskMonitorService, selectedAppPackage)) {
                Log.w(TAG, "App selecionado não está instalado: $selectedAppPackage")
                return@withContext
            }
            
            // Se forceOpen = true, sempre abre o app (mesmo que já esteja rodando)
            if (forceOpen) {
                Log.d(TAG, "Forçando abertura do app: $selectedAppPackage")
                AppUtils.launchApp(this@KioskMonitorService, selectedAppPackage)
                delay(2000) // Aguarda 2 segundos para o app abrir
            } else {
                // Verifica se o app está rodando
                if (!AppUtils.isAppRunning(this@KioskMonitorService, selectedAppPackage)) {
                    // Se não estiver rodando, abre o app
                    Log.d(TAG, "App não está rodando, abrindo: $selectedAppPackage")
                    AppUtils.launchApp(this@KioskMonitorService, selectedAppPackage)
                    delay(2000) // Aguarda 2 segundos para o app abrir
                }
            }
            
            // Traz o app para o foreground
            AppUtils.bringAppToForeground(this@KioskMonitorService, selectedAppPackage)
        }
    }
}

/**
 * Worker para monitoramento em background usando WorkManager
 * Útil como backup caso o serviço seja morto
 */
class KioskMonitorWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return try {
            val deviceId = DeviceUtils.getDeviceId(applicationContext)
            val kioskMode = SupabaseConfig.getKioskModeStatus(deviceId)
            
            if (kioskMode == true) {
                // Enforce kiosk mode
                val selectedAppPackage = PreferencesUtils.getSelectedAppPackage(applicationContext)
                if (selectedAppPackage != null && AppUtils.isAppInstalled(applicationContext, selectedAppPackage)) {
                    if (!AppUtils.isAppRunning(applicationContext, selectedAppPackage)) {
                        AppUtils.launchApp(applicationContext, selectedAppPackage)
                    } else {
                        AppUtils.bringAppToForeground(applicationContext, selectedAppPackage)
                    }
                }
            }
            
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
    
    companion object {
        fun startPeriodicWork(context: Context) {
            // Verifica a cada 1 minuto como backup (mínimo permitido pelo Android)
            val workRequest = PeriodicWorkRequestBuilder<KioskMonitorWorker>(
                15, TimeUnit.MINUTES // Mínimo é 15 minutos para PeriodicWorkRequest
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "kiosk_monitor_work",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
        
        /**
         * Inicia o serviço de monitoramento
         */
        fun start(context: Context) {
            val serviceIntent = Intent(context, KioskMonitorService::class.java)
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
            
            // Inicia também o WorkManager como backup
            startPeriodicWork(context)
        }
    }
}
