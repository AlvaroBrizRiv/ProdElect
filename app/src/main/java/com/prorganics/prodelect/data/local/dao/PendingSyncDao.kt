
package com.prorganics.prodelect.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.prorganics.prodelect.data.local.entity.PendingSyncEntity

/**
 * Gestiona la cola de operaciones pendientes
 * de sincronización con Firebase.
 */
@Dao
interface PendingSyncDao {

    // Registrar una operación pendiente.
    @Insert
    suspend fun insertPending(
        pending: PendingSyncEntity
    )

    // Consultar las operaciones pendientes en orden.
    @Query("SELECT * FROM pending_sync ORDER BY id ASC")
    suspend fun getPendingOperations(): List<PendingSyncEntity>

    // Eliminar una operación después de sincronizarla.
    @Query("DELETE FROM pending_sync WHERE id = :id")
    suspend fun deletePending(id: Long)

    // Consultar cuántas operaciones siguen pendientes.
    @Query("SELECT COUNT(*) FROM pending_sync")
    suspend fun countPending(): Int
}
