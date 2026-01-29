package com.gelafit.kioskcontroller

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.gelafit.kioskcontroller.util.PermissionStatus
import com.gelafit.kioskcontroller.util.PermissionUtils

class PermissionsActivity : AppCompatActivity() {
    
    private lateinit var permissionsContainer: LinearLayout
    private lateinit var refreshButton: Button
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permissions)
        
        permissionsContainer = findViewById(R.id.permissionsContainer)
        refreshButton = findViewById(R.id.refreshButton)
        
        refreshButton.setOnClickListener {
            checkPermissions()
        }
        
        checkPermissions()
    }
    
    override fun onResume() {
        super.onResume()
        // Atualiza quando volta para a tela (após conceder permissões)
        checkPermissions()
    }
    
    private fun checkPermissions() {
        permissionsContainer.removeAllViews()
        
        val permissions = PermissionUtils.checkAllPermissions(this)
        
        permissions.forEach { permission ->
            val permissionView = createPermissionView(permission)
            permissionsContainer.addView(permissionView)
        }
        
        // Mostra status geral
        val allGranted = PermissionUtils.areAllPermissionsGranted(this)
        val statusText = findViewById<TextView>(R.id.statusText)
        statusText.text = if (allGranted) {
            "✅ Todas as permissões foram concedidas!"
        } else {
            "⚠️ Algumas permissões precisam ser concedidas"
        }
        statusText.setTextColor(
            ContextCompat.getColor(
                this,
                if (allGranted) android.R.color.holo_green_dark else android.R.color.holo_orange_dark
            )
        )
    }
    
    private fun createPermissionView(permission: PermissionStatus): View {
        val view = layoutInflater.inflate(R.layout.item_permission, permissionsContainer, false)
        
        val nameText = view.findViewById<TextView>(R.id.permissionName)
        val descriptionText = view.findViewById<TextView>(R.id.permissionDescription)
        val statusText = view.findViewById<TextView>(R.id.permissionStatus)
        val actionButton = view.findViewById<Button>(R.id.permissionActionButton)
        
        nameText.text = permission.name
        descriptionText.text = permission.description
        
        if (permission.isGranted) {
            statusText.text = "✅ Concedida"
            statusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
            actionButton.visibility = View.GONE
        } else {
            statusText.text = "❌ Não concedida"
            statusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
            actionButton.text = "Conceder permissão"
            actionButton.setOnClickListener {
                PermissionUtils.openPermissionSettings(this, permission)
            }
            actionButton.visibility = View.VISIBLE
        }
        
        return view
    }
}
