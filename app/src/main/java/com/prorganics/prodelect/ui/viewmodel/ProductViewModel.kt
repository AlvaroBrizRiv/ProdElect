
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

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

    /** Texto de búsqueda controlado por la pantalla principal. */
    private val _consultaBusqueda = MutableStateFlow("")
    val consultaBusqueda: StateFlow<String> = _consultaBusqueda

    /**
     * Inventario de Firebase filtrado de forma reactiva.
     * Al cambiar la consulta o los productos, la lista se recalcula automáticamente.
     */
    val productosFiltrados: StateFlow<List<ProductFirebase>> =
        combine(productosNube, _consultaBusqueda) { inventario, consulta ->
            val texto = consulta.trim()
            if (texto.isEmpty()) {
                inventario
            } else {
                inventario.filter { producto ->
                    producto.nombre.contains(texto, ignoreCase = true) ||
                        producto.descripcion.contains(texto, ignoreCase = true) ||
                        producto.proveedor.contains(texto, ignoreCase = true)
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun actualizarBusqueda(texto: String) {
        _consultaBusqueda.value = texto
    }

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
     * Guarda en Room y en Firebase directamente para reflejar los cambios
     * de inmediato en ambas bases de datos.
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
                // 1. Guardar en Base de Datos Local (Room)
                val producto = ProductEntity(
                    id = id,
                    nombre = name,
                    descripcion = description,
                    cantidad = qty,
                    precio = price,
                    proveedor = supplier
                )

                val productoExistente = repository.getProductById(id)
                if (productoExistente == null) {
                    repository.insertProduct(producto)
                    Log.d("ProductViewModel", "Producto creado localmente: $id")
                } else {
                    repository.updateProduct(producto)
                    Log.d("ProductViewModel", "Producto actualizado localmente: $id")
                }

                // 2. Guardar en la Nube (Firestore) simultáneamente
                val firebaseEntity = ProductFirebase(
                    id = id,
                    nombre = name,
                    descripcion = description,
                    cantidad = qty,
                    precio = price,
                    proveedor = supplier
                )
                firebaseRepository.setProducto(firebaseEntity)

                _mensajeErrorRed.value = null

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("ProductViewModel", "Error al guardar producto", e)
                _mensajeErrorRed.value = "Error al guardar el producto: ${e.localizedMessage}"
            }
        }
    }

    /**
     * Eliminar producto.
     *
     * Elimina tanto de Room (Local) como de Firebase (Nube)
     * para asegurar la sincronización en tiempo real.
     */
    fun deleteProduct(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Eliminar de Room
                val producto = repository.getProductById(id)
                if (producto != null) {
                    repository.deleteProduct(producto)
                    Log.d("ProductViewModel", "Producto eliminado localmente: $id")
                } else {
                    Log.w("ProductViewModel", "Producto no encontrado localmente: $id")
                }

                // 2. Eliminar de Firebase
                firebaseRepository.deleteProducto(id)

                _mensajeErrorRed.value = null

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("ProductViewModel", "Error al eliminar producto", e)
                _mensajeErrorRed.value = "Error al eliminar el producto: ${e.localizedMessage}"
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
