package com.gelafit.kioskcontroller

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.gelafit.kioskcontroller.service.KioskMonitorService
import com.gelafit.kioskcontroller.service.KioskMonitorWorker
import com.gelafit.kioskcontroller.util.DeviceUtils
import com.gelafit.kioskcontroller.util.PermissionUtils
import com.gelafit.kioskcontroller.util.PreferencesUtils
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    
    private lateinit var deviceIdText: TextView
    private lateinit var statusText: TextView
    private lateinit var kioskModeText: TextView
    private lateinit var selectedAppText: TextView
    private lateinit var permissionsStatusText: TextView
    private lateinit var startServiceButton: Button
    private lateinit var stopServiceButton: Button
    private lateinit var selectAppButton: Button
    private lateinit var checkPermissionsButton: Button
    
    companion object {
        private const val REQUEST_SELECT_APP = 1001
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        initViews()
        setupClickListeners()
        displayDeviceInfo()
        checkServiceStatus()
        
        // Tenta iniciar automaticamente se já estiver configurado
        tryAutoStart()
    }
    
    private fun tryAutoStart() {
        // Se já tem app selecionado, permissões concedidas e serviço estava habilitado
        if (PreferencesUtils.hasSelectedApp(this) && 
            PermissionUtils.areAllPermissionsGranted(this) &&
            PreferencesUtils.isServiceEnabled(this)) {
            
            // Verifica se o serviço não está rodando
            if (!isServiceRunning(KioskMonitorService::class.java)) {
                Log.d("MainActivity", "Reiniciando serviço automaticamente")
                startKioskService()
            }
        }
    }
    
    private fun initViews() {
        deviceIdText = findViewById(R.id.deviceIdText)
        statusText = findViewById(R.id.statusText)
        kioskModeText = findViewById(R.id.kioskModeText)
        selectedAppText = findViewById(R.id.selectedAppText)
        permissionsStatusText = findViewById(R.id.permissionsStatusText)
        startServiceButton = findViewById(R.id.startServiceButton)
        stopServiceButton = findViewById(R.id.stopServiceButton)
        selectAppButton = findViewById(R.id.selectAppButton)
        checkPermissionsButton = findViewById(R.id.checkPermissionsButton)
    }
    
    private fun setupClickListeners() {
        startServiceButton.setOnClickListener {
            if (!PreferencesUtils.hasSelectedApp(this)) {
                Toast.makeText(this, "Selecione um aplicativo primeiro", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!PermissionUtils.areAllPermissionsGranted(this)) {
                Toast.makeText(this, "Conceda todas as permissões necessárias", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            startKioskService()
        }
        
        stopServiceButton.setOnClickListener {
            stopKioskService()
        }
        
        selectAppButton.setOnClickListener {
            val intent = Intent(this, AppSelectionActivity::class.java)
            startActivityForResult(intent, REQUEST_SELECT_APP)
        }
        
        checkPermissionsButton.setOnClickListener {
            val intent = Intent(this, PermissionsActivity::class.java)
            startActivity(intent)
        }
    }
    
    private fun displayDeviceInfo() {
        val deviceId = DeviceUtils.getDeviceId(this)
        deviceIdText.text = "Device ID: $deviceId"
        
        // Registra o dispositivo no Supabase
        lifecycleScope.launch {
            try {
                SupabaseConfig.registerDevice(deviceId)
                Toast.makeText(this@MainActivity, "Dispositivo registrado", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Erro ao registrar dispositivo: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun startKioskService() {
        KioskMonitorService.start(this)
        PreferencesUtils.setServiceEnabled(this, true)
        Toast.makeText(this, "Serviço iniciado em background", Toast.LENGTH_SHORT).show()
        checkServiceStatus()
    }
    
    private fun stopKioskService() {
        val serviceIntent = Intent(this, KioskMonitorService::class.java)
        stopService(serviceIntent)
        
        // Para o WorkManager também
        androidx.work.WorkManager.getInstance(this).cancelUniqueWork("kiosk_monitor_work")
        
        PreferencesUtils.setServiceEnabled(this, false)
        Toast.makeText(this, "Serviço parado", Toast.LENGTH_SHORT).show()
        checkServiceStatus()
    }
    
    private fun checkServiceStatus() {
        lifecycleScope.launch {
            val deviceId = DeviceUtils.getDeviceId(this@MainActivity)
            val kioskMode = SupabaseConfig.getKioskModeStatus(deviceId)
            
            kioskModeText.text = if (kioskMode == true) {
                "Modo Kiosk: ATIVO"
            } else {
                "Modo Kiosk: INATIVO"
            }
            
            // Verifica se o serviço está rodando
            val isRunning = isServiceRunning(KioskMonitorService::class.java)
            statusText.text = if (isRunning) {
                "Status: Serviço ATIVO"
            } else {
                "Status: Serviço INATIVO"
            }
        }
    }
    
    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val activityManager = getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager
        val services = activityManager.getRunningServices(Integer.MAX_VALUE)
        
        return services.any { it.service.className == serviceClass.name }
    }
    
    override fun onResume() {
        super.onResume()
        updateSelectedApp()
        updatePermissionsStatus()
        checkServiceStatus()
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_SELECT_APP && resultCode == RESULT_OK) {
            updateSelectedApp()
            Toast.makeText(this, "App selecionado com sucesso", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun updateSelectedApp() {
        val selectedAppName = PreferencesUtils.getSelectedAppName(this)
        val selectedAppPackage = PreferencesUtils.getSelectedAppPackage(this)
        
        if (selectedAppName != null && selectedAppPackage != null) {
            selectedAppText.text = "App selecionado: $selectedAppName\n($selectedAppPackage)"
        } else {
            selectedAppText.text = "Nenhum app selecionado"
        }
    }
    
    private fun updatePermissionsStatus() {
        val allGranted = PermissionUtils.areAllPermissionsGranted(this)
        permissionsStatusText.text = if (allGranted) {
            "✅ Todas as permissões concedidas"
        } else {
            "⚠️ Permissões pendentes - Clique para verificar"
        }
    }
}
