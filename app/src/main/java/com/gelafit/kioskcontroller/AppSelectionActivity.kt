package com.gelafit.kioskcontroller

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gelafit.kioskcontroller.util.AppUtils
import com.gelafit.kioskcontroller.util.InstalledApp
import com.gelafit.kioskcontroller.util.PreferencesUtils

class AppSelectionActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AppListAdapter
    private var installedApps: List<InstalledApp> = emptyList()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_selection)
        
        recyclerView = findViewById(R.id.appsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        loadApps()
    }
    
    private fun loadApps() {
        installedApps = AppUtils.getUserApps(this)
        adapter = AppListAdapter(installedApps) { app ->
            PreferencesUtils.saveSelectedApp(this, app.packageName, app.appName)
            setResult(RESULT_OK)
            finish()
        }
        recyclerView.adapter = adapter
    }
    
    inner class AppListAdapter(
        private val apps: List<InstalledApp>,
        private val onAppSelected: (InstalledApp) -> Unit
    ) : RecyclerView.Adapter<AppListAdapter.AppViewHolder>() {
        
        inner class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val appIcon: ImageView = itemView.findViewById(R.id.appIcon)
            val appName: TextView = itemView.findViewById(R.id.appName)
            val packageName: TextView = itemView.findViewById(R.id.packageName)
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_app, parent, false)
            return AppViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
            val app = apps[position]
            
            holder.appIcon.setImageDrawable(app.icon)
            holder.appName.text = app.appName
            holder.packageName.text = app.packageName
            
            holder.itemView.setOnClickListener {
                onAppSelected(app)
            }
        }
        
        override fun getItemCount(): Int = apps.size
    }
}
