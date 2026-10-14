package com.prorganics.prodelect.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.prorganics.prodelect.data.remote.entity.ProductFirebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Repositorio para manejar las operaciones con Firebase Firestore.
 * 
 * Sigue el patrón Repository para abstraer el origen de los datos de la UI.
 * Implementa operaciones CRUD completas y un listener en tiempo real (Sincronización simultánea).
 */
class FirebaseRepository {

    private val db = FirebaseFirestore.getInstance()
    private val productosCollection = db.collection("productos")

    /**
     * CREATE / UPDATE (set): Guarda o actualiza un producto en Firestore.
     * Utiliza set() con SetOptions.merge() para hacer "upsert" (si existe actualiza, si no, crea).
     */
    suspend fun setProducto(producto: ProductFirebase) {
        withContext(Dispatchers.IO) {
            try {
                val docRef = if (producto.id.isEmpty()) {
                    productosCollection.document()
                } else {
                    productosCollection.document(producto.id)
                }
                docRef.set(producto, SetOptions.merge()).await()
                Log.d("FirebaseRepository", "✅ Producto guardado/actualizado: ${docRef.id}")
            } catch (e: Exception) {
                Log.e("FirebaseRepository", "❌ Error al guardar el producto", e)
            }
        }
    }

    /**
     * READ (get): Obtiene un producto específico por su ID una sola vez.
     */
    suspend fun getProductoById(productId: String): ProductFirebase? {
        return withContext(Dispatchers.IO) {
            try {
                val snapshot = productosCollection.document(productId).get().await()
                snapshot.toObject(ProductFirebase::class.java)
            } catch (e: Exception) {
                Log.e("FirebaseRepository", "❌ Error al obtener el producto $productId", e)
                null
            }
        }
    }

    /**
     * UPDATE explícito (update): Actualiza campos específicos de un producto sin sobreescribir el resto.
     */
    suspend fun updateProducto(productId: String, updates: Map<String, Any>) {
        withContext(Dispatchers.IO) {
            try {
                productosCollection.document(productId).update(updates).await()
                Log.d("FirebaseRepository", "✅ Producto actualizado: $productId")
            } catch (e: Exception) {
                Log.e("FirebaseRepository", "❌ Error al actualizar el producto", e)
            }
        }
    }

    /**
     * DELETE (delete): Elimina un producto por su ID de la nube.
     */
    suspend fun deleteProducto(productId: String) {
        withContext(Dispatchers.IO) {
            try {
                productosCollection.document(productId).delete().await()
                Log.d("FirebaseRepository", "✅ Producto eliminado: $productId")
            } catch (e: Exception) {
                Log.e("FirebaseRepository", "❌ Error al eliminar el producto", e)
            }
        }
    }

    /**
     * BATCH DELETE: Elimina todos los productos de la colección.
     */
    suspend fun deleteAllProductos() {
        withContext(Dispatchers.IO) {
            try {
                val snapshot = productosCollection.get().await()
                val batch = db.batch()
                for (doc in snapshot.documents) {
                    batch.delete(doc.reference)
                }
                batch.commit().await()
                Log.d("FirebaseRepository", "✅ Todos los productos eliminados de la nube")
            } catch (e: Exception) {
                Log.e("FirebaseRepository", "❌ Error eliminando todos los productos", e)
            }
        }
    }

    /**
     * REALTIME LISTENER: Detecta cambios simultáneos de varios usuarios (vendedor y encargado de almacén).
     * Convierte la API basada en callbacks de Firebase (addSnapshotListener) a un Flow reactivo de Kotlin,
     * permitiendo conectarlo limpiamente con la UI a través del ViewModel.
     */
    fun getAllProductosRealTime(): Flow<List<ProductFirebase>> = callbackFlow {
        // addSnapshotListener suscribe a la app a cualquier evento de la base de datos
        val subscription = productosCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("FirebaseRepository", "❌ Error en SnapshotListener", error)
                close(error) // Cierra el flujo y emite el error
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val productos = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(ProductFirebase::class.java)
                }
                // Envía la nueva lista sincronizada a los observadores
                trySend(productos).isSuccess
            }
        }

        // Se invoca automáticamente cuando se cancela el scope (ej. al salir de la app/pantalla).
        // Evita fugas de memoria limpiando el listener de Firestore (Disponibilidad).
        awaitClose {
            subscription.remove()
        }
    }
}