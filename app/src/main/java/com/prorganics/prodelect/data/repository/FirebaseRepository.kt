package com.prorganics.prodelect.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.prorganics.prodelect.data.remote.entity.ProductFirebase
import kotlinx.coroutines.tasks.await

/**
 * Repositorio para manejar las operaciones con Firebase Firestore.
 * 
 * Sigue el patrón Repository para abstraer el origen de los datos de la UI.
 */
class FirebaseRepository {

    // Inicializar la instancia de Firestore
    private val db = FirebaseFirestore.getInstance()
    // Referencia a la colección 'productos'
    private val productosCollection = db.collection("productos")

    /**
     * Agrega un producto de prueba a la colección 'productos'.
     * Utiliza corrutinas y la función de extensión 'await()' para manejar
     * la tarea de Firebase de forma síncrona dentro de la corrutina.
     */
    suspend fun agregarProductoDePrueba() {
        // Crear un objeto ProductFirebase con datos dummy
        val productoDePrueba = ProductFirebase(
            nombre = "Microcontrolador ESP32",
            descripcion = "Módulo Wi-Fi + Bluetooth para proyectos IoT",
            cantidad = 50,
            precio = 12.50,
            proveedor = "ElecSupplies Inc."
        )

        try {
            // Se usa document() sin argumentos para que Firestore genere un ID automático.
            // set() escribe el objeto en el documento.
            productosCollection.document().set(productoDePrueba).await()
            Log.d("FirebaseRepository", "✅ ¡Producto de prueba añadido exitosamente a Firestore!")
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "❌ Error al añadir el producto de prueba", e)
        }
    }
}