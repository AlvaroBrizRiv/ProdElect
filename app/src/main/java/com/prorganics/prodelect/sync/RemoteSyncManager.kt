
package com.prorganics.prodelect.sync

import android.util.Log
import androidx.room.withTransaction
import com.prorganics.prodelect.data.local.AppDatabase
import com.prorganics.prodelect.data.local.dao.PendingSyncDao
import com.prorganics.prodelect.data.local.dao.ProductoDao
import com.prorganics.prodelect.data.local.entity.ProductEntity
import com.prorganics.prodelect.data.remote.entity.ProductFirebase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sincroniza productos recibidos desde Firebase hacia Room.
 *
 * Protege las modificaciones locales que todavía tienen
 * operaciones pendientes en pending_sync.
 *
 * No realiza escrituras en Firebase ni elimina productos
 * locales que no aparezcan en la lista remota.
 */
@Singleton
class RemoteSyncManager @Inject constructor(
    private val database: AppDatabase,
    private val productoDao: ProductoDao,
    private val pendingSyncDao: PendingSyncDao
) {

    private val mutex = Mutex()

    /**
     * Recibe una lista de productos remotos y la incorpora
     * a SQLite cuando no existen cambios locales pendientes.
     */
    suspend fun actualizarDesdeFirebase(
        productosRemotos: List<ProductFirebase>
    ) {
        mutex.withLock {
            withContext(Dispatchers.IO) {
                try {
                    var actualizados = 0
                    var protegidos = 0
                    var omitidos = 0

                    for (remoto in productosRemotos) {

                        val id = remoto.id.trim()

                        if (id.isBlank()) {
                            Log.w(
                                "RemoteSyncManager",
                                "Producto remoto sin ID: omitido"
                            )
                            omitidos++
                            continue
                        }

                        database.withTransaction {

                            val pendientes =
                                pendingSyncDao.countPendingByProductId(id)

                            if (pendientes > 0) {
                                // No sobrescribir cambios locales
                                // pendientes de enviar.
                                protegidos++
                            } else {
                                val productoLocal = ProductEntity(
                                    id = id,
                                    nombre = remoto.nombre,
                                    descripcion = remoto.descripcion,
                                    cantidad = remoto.cantidad,
                                    precio = remoto.precio,
                                    proveedor = remoto.proveedor
                                )

                                // Escritura directa en DAO:
                                // no genera una nueva operación pendiente.
                                productoDao.insertProduct(productoLocal)
                                actualizados++
                            }
                        }
                    }

                    Log.d(
                        "RemoteSyncManager",
                        "Recepción finalizada. Actualizados: " +
                                "$actualizados, protegidos: $protegidos, " +
                                "omitidos: $omitidos"
                    )

                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(
                        "RemoteSyncManager",
                        "Error al actualizar SQLite desde Firebase",
                        e
                    )
                    throw e
                }
            }
        }
    }
}
