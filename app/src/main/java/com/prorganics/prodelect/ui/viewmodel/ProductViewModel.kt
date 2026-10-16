
package com.prorganics.prodelect.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prorganics.prodelect.data.local.entity.ProductEntity
import com.prorganics.prodelect.data.remote.api.ProductApiService
import com.prorganics.prodelect.data.remote.api.ProductDto
import com.prorganics.prodelect.data.remote.entity.ProductFirebase
import com.prorganics.prodelect.data.repository.FirebaseRepository
import com.prorganics.prodelect.data.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel principal de productos de ProdElect.
 *
 * Integra:
 * - Persistencia local con Room / SQLite
 * - Operaciones pendientes para SyncManager
 * - Lectura en tiempo real desde Firebase Firestore
 * - Interoperabilidad JSON / REST mediante Retrofit
 * - Manejo de estado reactivo con StateFlow
 *
 * Las operaciones de escritura se registran primero
 * en Room. SyncManager realizará posteriormente
 * la sincronización hacia Firebase.
 */
@HiltViewModel
class ProductViewModel @Inject constructor(
    private val repository: ProductRepository,
    private val firebaseRepository: FirebaseRepository,
    private val apiService: ProductApiService
) : ViewModel() {

    /**
     * Productos obtenidos de Firebase en tiempo real.
     *
     * Conserva la funcionalidad implementada
     * por el equipo.
     */
    val productosNube: StateFlow<List<ProductFirebase>> =
        firebaseRepository.getAllProductosRealTime()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    /**
     * Productos almacenados localmente en Room.
     *
     * Permite observar cambios en SQLite.
     */
    val productos: StateFlow<List<ProductEntity>> =
        repository.getAllProducts()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    /**
     * Catálogo externo obtenido mediante JSON / REST.
     */
    private val _catalogoExterno =
        MutableStateFlow<List<ProductDto>>(emptyList())

    val catalogoExterno: StateFlow<List<ProductDto>> =
        _catalogoExterno

    /**
     * Mensajes de error para la interfaz.
     */
    private val _mensajeErrorRed =
        MutableStateFlow<String?>(null)

    val mensajeErrorRed: StateFlow<String?> =
        _mensajeErrorRed

    /**
     * Crear o actualizar un producto.
     *
     * Guarda primero en Room y registra la operación
     * correspondiente en pending_sync.
     *
     * No escribe directamente en Firebase para
     * evitar duplicaciones con SyncManager.
     */
    fun saveProduct(
        id: String,
        name: String,
        description: String,
        qty: Int,
        price: Double,
        supplier: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val producto = ProductEntity(
                    id = id,
                    nombre = name,
                    descripcion = description,
                    cantidad = qty,
                    precio = price,
                    proveedor = supplier
                )

                val productoExistente =
                    repository.getProductById(id)

                if (productoExistente == null) {
                    repository.insertProduct(producto)

                    Log.d(
                        "ProductViewModel",
                        "Producto creado localmente: $id"
                    )
                } else {
                    repository.updateProduct(producto)

                    Log.d(
                        "ProductViewModel",
                        "Producto actualizado localmente: $id"
                    )
                }

                _mensajeErrorRed.value = null

                Log.d(
                    "ProductViewModel",
                    "Operación registrada para sincronización"
                )

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(
                    "ProductViewModel",
                    "Error al guardar producto",
                    e
                )

                _mensajeErrorRed.value =
                    "Error al guardar el producto: ${e.localizedMessage}"
            }
        }
    }

    /**
     * Eliminar producto.
     *
     * Elimina de Room y registra DELETE
     * en la cola de sincronización.
     */
    fun deleteProduct(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val producto =
                    repository.getProductById(id)

                if (producto != null) {
                    repository.deleteProduct(producto)

                    Log.d(
                        "ProductViewModel",
                        "Producto eliminado localmente: $id"
                    )
                } else {
                    Log.w(
                        "ProductViewModel",
                        "Producto no encontrado localmente: $id"
                    )
                }

                _mensajeErrorRed.value = null

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(
                    "ProductViewModel",
                    "Error al eliminar producto",
                    e
                )

                _mensajeErrorRed.value =
                    "Error al eliminar el producto: ${e.localizedMessage}"
            }
        }
    }

    /**
     * Método de inicialización de datos de prueba.
     *
     * DESACTIVADO POR SEGURIDAD.
     *
     * La implementación original eliminaba todos los
     * productos de Firestore y de SQLite antes de
     * insertar un producto de prueba.
     *
     * Se conserva el método para compatibilidad,
     * pero sin ejecutar operaciones destructivas.
     */
    @Deprecated(
        message = "Desactivado para proteger los datos compartidos"
    )
    fun prepopulateDatabase() {
        Log.w(
            "ProductViewModel",
            "prepopulateDatabase desactivado: no se modificaron datos"
        )
    }

    /**
     * Consultar catálogo de un proveedor externo.
     *
     * Conserva la interoperabilidad JSON / REST
     * desarrollada por el equipo.
     */
    fun descargarCatalogoProveedor() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d(
                    "ProductViewModel",
                    "Iniciando descarga del catálogo externo"
                )

                val catalogoTraidoDeInternet =
                    apiService.getProductos()

                _catalogoExterno.value =
                    catalogoTraidoDeInternet

                _mensajeErrorRed.value = null

                Log.d(
                    "ProductViewModel",
                    "Catálogo externo descargado: " +
                            "${catalogoTraidoDeInternet.size} productos"
                )

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(
                    "ProductViewModel",
                    "Error al descargar catálogo externo",
                    e
                )

                _mensajeErrorRed.value =
                    "Error al conectar con el servidor: " +
                            "${e.localizedMessage}"
            }
        }
    }
}
