
package com.prorganics.prodelect.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Registra las operaciones pendientes de sincronización
 * para enviarlas a Firebase cuando exista conexión.
 */
@Entity(tableName = "pending_sync")
data class PendingSyncEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val productId: String,
    val operation: String,
    val createdAt: Long = System.currentTimeMillis()
)
