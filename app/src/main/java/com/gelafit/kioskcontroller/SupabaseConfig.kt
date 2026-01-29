package com.gelafit.kioskcontroller

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SupabaseConfig {
    private const val SUPABASE_URL = "https://kihyhoqbrkwbfudttevo.supabase.co"
    private const val SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImtpaHlob3Ficmt3YmZ1ZHR0ZXZvIiwicm9sZSI6ImFub24iLCJpYXQiOjE3MTU1NTUwMjcsImV4cCI6MjAzMTEzMTAyN30.XtBTlSiqhsuUIKmhAMEyxofV-dRst7240n912m4O4Us"
    
    // TODO: Substitua pelo package name do seu app Flutter
    const val FLUTTER_APP_PACKAGE = "com.gelafit.app"
    
    val client: SupabaseClient = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_KEY
    ) {
        install(Postgrest)
    }
    
    /**
     * Busca o status do kiosk_mode para um device_id específico
     */
    suspend fun getKioskModeStatus(deviceId: String): Boolean? {
        return withContext(Dispatchers.IO) {
            try {
                val response = client.postgrest.from("devices")
                    .select {
                        filter {
                            eq("device_id", deviceId)
                        }
                    }
                    .decodeSingle<DeviceStatus>()
                
                // Atualiza last_seen
                updateLastSeen(deviceId)
                
                response.kiosk_mode
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
    
    /**
     * Atualiza o campo last_seen do dispositivo
     */
    private suspend fun updateLastSeen(deviceId: String) {
        try {
            client.postgrest.from("devices")
                .update(mapOf("last_seen" to "now()")) {
                    filter {
                        eq("device_id", deviceId)
                    }
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Registra ou atualiza um dispositivo na tabela
     */
    suspend fun registerDevice(deviceId: String, unitName: String? = null) {
        withContext(Dispatchers.IO) {
            try {
                // Verifica se já existe
                val existing = try {
                    client.postgrest.from("devices")
                        .select {
                            filter {
                                eq("device_id", deviceId)
                            }
                        }
                        .decodeSingle<DeviceStatus>()
                    true
                } catch (e: Exception) {
                    false
                }
                
                if (!existing) {
                    // Cria novo registro
                    client.postgrest.from("devices")
                        .insert(DeviceInsert(
                            device_id = deviceId,
                            unit_name = unitName,
                            is_active = true,
                            kiosk_mode = false
                        ))
                } else {
                    // Atualiza is_active
                    client.postgrest.from("devices")
                        .update(mapOf("is_active" to true)) {
                            filter {
                                eq("device_id", deviceId)
                            }
                        }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

data class DeviceStatus(
    val id: String,
    val device_id: String,
    val unit_name: String?,
    val is_active: Boolean,
    val kiosk_mode: Boolean,
    val last_seen: String?,
    val registered_at: String?
)

data class DeviceInsert(
    val device_id: String,
    val unit_name: String?,
    val is_active: Boolean,
    val kiosk_mode: Boolean
)
