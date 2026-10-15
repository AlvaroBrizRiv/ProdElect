
package com.prorganics.prodelect.sync

import android.util.Log
import com.prorganics.prodelect.data.local.dao.PendingSyncDao
import com.prorganics.prodelect.data.local.dao.ProductoDao
import com.prorganics.prodelect.data.repository.FirebaseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Administrador de sincronizacion de ProdElect.
 *
 * Procesa las operaciones pendientes almacenadas
 * en SQLite (Room) y las envia a Firebase Firestore.
 *
 * Operaciones:
 * CREATE: Crear producto en Firebase.
 * UPDATE: Actualizar producto en Firebase.
 * DELETE: Eliminar producto de Firebase.
 *
 * Las operaciones se eliminan de la cola solamente
 * despues de completarse correctamente.
 */
@Singleton
class SyncManager @Inject constructor(
    private val pendingSyncDao: PendingSyncDao,
    private val productoDao: ProductoDao,
    private val firebaseRepository: FirebaseRepository
) {

    // Evita ejecuciones simultaneas del sincronizador.
    private val mutex = Mutex()

    /**
     * Procesa las operaciones pendientes en orden.
     */
    suspend fun sincronizarPendientes() {

        mutex.withLock {

            withContext(Dispatchers.IO) {

                val pendientes =
                    pendingSyncDao.getPendingOperations()

                Log.d(
                    "SyncManager",
                    "Operaciones pendientes: ${pendientes.size}"
                )

                for (pendiente in pendientes) {

                    try {

                        when (pendiente.operation) {

                            "CREATE", "UPDATE" -> {

                                val producto =
                                    productoDao.getProductById(
                                        pendiente.productId
                                    )

                                if (producto != null) {

                                    firebaseRepository.guardarProducto(
                                        producto
                                    )

                                    Log.d(
                                        "SyncManager",
                                        "Producto enviado: ${producto.id}"
                                    )

                                } else {

                                    Log.w(
                                        "SyncManager",
                                        "Producto no encontrado: ${pendiente.productId}"
                                    )

                                    // Conserva la operacion pendiente.
                                    break
                                }
                            }

                            "DELETE" -> {

                                firebaseRepository.eliminarProducto(
                                    pendiente.productId
                                )

                                Log.d(
                                    "SyncManager",
                                    "Producto eliminado de Firebase: ${pendiente.productId}"
                                )
                            }

                            else -> {

                                Log.e(
                                    "SyncManager",
                                    "Operacion desconocida: ${pendiente.operation}"
                                )

                                // No eliminar operaciones desconocidas.
                                break
                            }
                        }

                        // Se elimina solamente despues del exito.
                        pendingSyncDao.deletePending(
                            pendiente.id
                        )

                        Log.d(
                            "SyncManager",
                            "Operacion sincronizada: ${pendiente.id}"
                        )

                    } catch (e: CancellationException) {

                        throw e

                    } catch (e: Exception) {

                        Log.e(
                            "SyncManager",
                            "Error al sincronizar operacion ${pendiente.id}",
                            e
                        )

                        // Conservar la operacion para reintentar.
                        break
                    }
                }

                val restantes =
                    pendingSyncDao.countPending()

                Log.d(
                    "SyncManager",
                    "Operaciones restantes: $restantes"
                )
            }
        }
    }
}
