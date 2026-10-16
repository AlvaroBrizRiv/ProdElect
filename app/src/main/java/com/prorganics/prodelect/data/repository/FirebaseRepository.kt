
package com.prorganics.prodelect.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.prorganics.prodelect.data.local.entity.ProductEntity
import com.prorganics.prodelect.data.remote.entity.ProductFirebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Repositorio remoto de ProdElect.
 *
 * Integra:
 * - CRUD de Firestore
 * - Lectura en tiempo real
 * - Sincronización mediante UUID de Room
 * - Consulta de existencia de documentos
 */
class FirebaseRepository {

    private val db = FirebaseFirestore.getInstance()

    private val productosCollection = db.collection("productos")

    // Convertir producto local al formato de Firebase
    private fun convertirAFirebase(
        producto: ProductEntity
    ): ProductFirebase {
        return ProductFirebase(
            id = producto.id,
            nombre = producto.nombre,
            descripcion = producto.descripcion,
            cantidad = producto.cantidad,
            precio = producto.precio,
            proveedor = producto.proveedor
        )
    }

    /**
     * Método utilizado por SyncManager.
     * Conserva el UUID de SQLite.
     */
    suspend fun guardarProducto(producto: ProductEntity) {
        val productoFirebase = convertirAFirebase(producto)
        setProducto(productoFirebase)
    }

    /**
     * Crear o actualizar un producto en Firestore.
     * Método utilizado por ProductViewModel.
     *
     * Los errores se propagan para que el llamador
     * pueda detectar fallos de sincronización.
     */
    suspend fun setProducto(producto: ProductFirebase) {
        withContext(Dispatchers.IO) {
            try {
                val docRef = if (producto.id.isEmpty()) {
                    productosCollection.document()
                } else {
                    productosCollection.document(producto.id)
                }

                docRef.set(
                    producto,
                    SetOptions.merge()
                ).await()

                Log.d(
                    "FirebaseRepository",
                    "Producto guardado: ${docRef.id}"
                )

            } catch (e: Exception) {
                Log.e(
                    "FirebaseRepository",
                    "Error al guardar producto",
                    e
                )
                throw e
            }
        }
    }

    /**
     * Obtener un producto específico desde Firestore.
     */
    suspend fun getProductoById(
        productId: String
    ): ProductFirebase? {
        return withContext(Dispatchers.IO) {
            try {
                val documento = productosCollection
                    .document(productId)
                    .get()
                    .await()

                documento.toObject(
                    ProductFirebase::class.java
                )

            } catch (e: Exception) {
                Log.e(
                    "FirebaseRepository",
                    "Error al consultar producto $productId",
                    e
                )
                throw e
            }
        }
    }

    /**
     * Actualizar campos específicos de un producto.
     */
    suspend fun updateProducto(
        productId: String,
        updates: Map<String, Any>
    ) {
        withContext(Dispatchers.IO) {
            try {
                productosCollection
                    .document(productId)
                    .update(updates)
                    .await()

                Log.d(
                    "FirebaseRepository",
                    "Producto actualizado: $productId"
                )

            } catch (e: Exception) {
                Log.e(
                    "FirebaseRepository",
                    "Error al actualizar producto",
                    e
                )
                throw e
            }
        }
    }

    /**
     * Método utilizado por ProductViewModel.
     */
    suspend fun deleteProducto(productId: String) {
        withContext(Dispatchers.IO) {
            try {
                productosCollection
                    .document(productId)
                    .delete()
                    .await()

                Log.d(
                    "FirebaseRepository",
                    "Producto eliminado: $productId"
                )

            } catch (e: Exception) {
                Log.e(
                    "FirebaseRepository",
                    "Error al eliminar producto",
                    e
                )
                throw e
            }
        }
    }

    /**
     * Método utilizado por SyncManager.
     */
    suspend fun eliminarProducto(productId: String) {
        deleteProducto(productId)
    }

    /**
     * Eliminar todos los productos remotos.
     *
     * ADVERTENCIA: Operación destructiva.
     * No ejecutar en la base compartida durante pruebas.
     */
    suspend fun deleteAllProductos() {
        withContext(Dispatchers.IO) {
            try {
                val snapshot = productosCollection
                    .get()
                    .await()

                val documentos = snapshot.documents

                for (grupo in documentos.chunked(500)) {
                    val batch = db.batch()

                    grupo.forEach { documento ->
                        batch.delete(documento.reference)
                    }

                    batch.commit().await()
                }

                Log.d(
                    "FirebaseRepository",
                    "Eliminación remota completada"
                )

            } catch (e: Exception) {
                Log.e(
                    "FirebaseRepository",
                    "Error al eliminar productos",
                    e
                )
                throw e
            }
        }
    }

    /**
     * Escucha de productos en tiempo real.
     * Funcionalidad desarrollada por el equipo.
     */
    fun getAllProductosRealTime(): Flow<List<ProductFirebase>> =
        callbackFlow {

            val subscription = productosCollection
                .addSnapshotListener { snapshot, error ->

                    if (error != null) {
                        Log.e(
                            "FirebaseRepository",
                            "Error en escucha de Firestore",
                            error
                        )
                        close(error)
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        val productos = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(ProductFirebase::class.java)
                        }

                        trySend(productos)
                    }
                }

            awaitClose {
                subscription.remove()
            }
        }

    /**
     * Verificar si un documento existe en Firestore.
     * Solo lectura.
     */
    suspend fun verificarExistenciaProducto(
        productId: String
    ): Boolean {
        val documento = productosCollection
            .document(productId)
            .get()
            .await()

        Log.d(
            "FirebaseRepository",
            "Producto $productId existe: ${documento.exists()}"
        )

        return documento.exists()
    }

    /**
     * Método de prueba original.
     * Se conserva por compatibilidad.
     * No ejecutar sin autorización del equipo.
     */
    suspend fun agregarProductoDePrueba() {
        val productoDePrueba = ProductFirebase(
            nombre = "Microcontrolador ESP32",
            descripcion = "Modulo Wi-Fi + Bluetooth para proyectos IoT",
            cantidad = 50,
            precio = 12.50,
            proveedor = "ElecSupplies Inc."
        )

        setProducto(productoDePrueba)
    }
}
