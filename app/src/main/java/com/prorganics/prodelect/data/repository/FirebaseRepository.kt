
package com.prorganics.prodelect.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.prorganics.prodelect.data.local.entity.ProductEntity
import com.prorganics.prodelect.data.remote.entity.ProductFirebase
import kotlinx.coroutines.tasks.await

/**
 * Repositorio remoto de ProdElect.
 *
 * Gestiona las operaciones de productos
 * en Cloud Firestore.
 */
class FirebaseRepository {

    private val db = FirebaseFirestore.getInstance()

    private val productosCollection =
        db.collection("productos")

    /**
     * Convierte un producto local al formato de Firebase.
     */
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
     * Crea o actualiza un producto en Firestore
     * utilizando el mismo ID de SQLite.
     */
    suspend fun guardarProducto(
        producto: ProductEntity
    ) {
        val productoFirebase = convertirAFirebase(producto)

        productosCollection
            .document(producto.id)
            .set(productoFirebase)
            .await()

        Log.d(
            "FirebaseRepository",
            "Producto sincronizado: ${producto.id}"
        )
    }

    /**
     * Elimina un producto remoto mediante su ID.
     */
    suspend fun eliminarProducto(
        productId: String
    ) {
        productosCollection
            .document(productId)
            .delete()
            .await()

        Log.d(
            "FirebaseRepository",
            "Producto eliminado: $productId"
        )
    }

    /**
     * Metodo de prueba original.
     * Se conserva para mantener compatibilidad.
     */
    suspend fun agregarProductoDePrueba() {
        val productoDePrueba = ProductFirebase(
            nombre = "Microcontrolador ESP32",
            descripcion = "Modulo Wi-Fi + Bluetooth para proyectos IoT",
            cantidad = 50,
            precio = 12.50,
            proveedor = "ElecSupplies Inc."
        )

        try {
            productosCollection
                .document()
                .set(productoDePrueba)
                .await()

            Log.d(
                "FirebaseRepository",
                "Producto de prueba agregado a Firestore"
            )
        } catch (e: Exception) {
            Log.e(
                "FirebaseRepository",
                "Error al agregar producto de prueba",
                e
            )
        }
    }
}
