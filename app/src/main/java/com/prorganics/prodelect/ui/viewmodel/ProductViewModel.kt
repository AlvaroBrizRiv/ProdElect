package com.prorganics.prodelect.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prorganics.prodelect.data.local.entity.ProductEntity
import com.prorganics.prodelect.data.remote.api.ProductApiService
import com.prorganics.prodelect.data.remote.api.ProductDto
import com.prorganics.prodelect.data.remote.entity.ProductFirebase
import com.prorganics.prodelect.data.repository.FirebaseRepository
import com.prorganics.prodelect.data.repository.ProductRepository
import kotlinx.coroutines.Dispatchers
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel que actúa como enlace entre el Repositorio de datos y la UI (Jetpack Compose).
 *
 * Principios aplicados:
 * - Unidirectional Data Flow (UDF): El estado fluye hacia abajo (UI) y los eventos fluyen hacia arriba (ViewModel).
 * - Lifecycle Awareness: Mantiene el estado persistente durante los cambios de configuración (rotación de pantalla).
 * - Reactividad y Disponibilidad: Se utiliza [StateFlow] para emitir siempre el último estado a la vista.
 * - Inyección de dependencias (Hilt): El @HiltViewModel inyecta automáticamente el ProductRepository y el FirebaseRepository.
 */
@HiltViewModel
class ProductViewModel @Inject constructor(
    private val repository: ProductRepository,
    private val firebaseRepository: FirebaseRepository,
    private val apiService: ProductApiService
) : ViewModel() {

    /**
     * Flujo de estado persistente (StateFlow) que contiene la lista de inventario sincronizada desde FIRESTORE.
     *
     * Transforma el 'Flow' reactivo de la base de datos de Firestore a un 'StateFlow' seguro para Compose.
     * Usando 'firebaseRepository.getAllProductosRealTime()' garantizamos que cualquier cambio en la web u otros dispositivos
     * se refleje instantáneamente en esta UI.
     */
    val productosNube: StateFlow<List<ProductFirebase>> =
        firebaseRepository.getAllProductosRealTime()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    /**
     * Flujo local de Room (Opcional, para la persistencia offline de contingencia)
     */
    val productos: StateFlow<List<ProductEntity>> = repository.getAllProducts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Estado para el Catálogo de Proveedores Externos (Internet HTTP).
     * Usamos MutableStateFlow porque nosotros vamos a empujar los datos hacia él
     * una vez que el servidor nos responda.
     */
    private val _catalogoExterno = MutableStateFlow<List<ProductDto>>(emptyList())
    val catalogoExterno: StateFlow<List<ProductDto>> = _catalogoExterno

    /**
     * Estado para manejar errores de red o tiempos de carga (Loading).
     * Vital para la experiencia de usuario (UX).
     */
    private val _mensajeErrorRed = MutableStateFlow<String?>(null)
    val mensajeErrorRed: StateFlow<String?> = _mensajeErrorRed

    /**
     * Evento UI: Guardar o Actualizar un producto en el inventario.
     * Esto escribe la información simultáneamente en Room (Local) y Firestore (Nube).
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
            // Guardar en Base de Datos Local (Room)
            val entity = ProductEntity(
                id = id,
                nombre = name,
                descripcion = description,
                cantidad = qty,
                precio = price,
                proveedor = supplier
            )
            repository.insertProduct(entity)

            // Guardar en la Nube (Firestore)
            val firebaseEntity = ProductFirebase(
                id = id,
                nombre = name,
                descripcion = description,
                cantidad = qty,
                precio = price,
                proveedor = supplier
            )
            firebaseRepository.setProducto(firebaseEntity)
        }
    }

    /**
     * Evento UI: Eliminar un producto.
     */
    fun deleteProduct(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            // Eliminar de Room
            repository.getProductById(id)?.let {
                repository.deleteProduct(it)
            }
            // Eliminar de Firebase
            firebaseRepository.deleteProducto(id)
        }
    }

    /**
     * Limpia la base de datos (Nube y Local) y agrega el producto específico de prueba.
     */
    fun prepopulateDatabase() {
        viewModelScope.launch(Dispatchers.IO) {
            // 1. Eliminar todos los registros existentes
            firebaseRepository.deleteAllProductos()
            repository.deleteAllProducts()

            // 2. Insertar el nuevo producto en la nube
            val nuevoProductoNube = ProductFirebase(
                nombre = "Mouse Logitech M185",
                descripcion = "Mouse inhalambrico USB.",
                cantidad = 15,
                precio = 12.990,
                proveedor = "Logitech Chile"
            )
            firebaseRepository.setProducto(nuevoProductoNube)

            // 3. Insertar el nuevo producto en Room local
            val nuevoProductoLocal = ProductEntity(
                nombre = "Mouse Logitech M185",
                descripcion = "Mouse inhalambrico USB.",
                cantidad = 15,
                precio = 12.990,
                proveedor = "Logitech Chile"
            )
            repository.insertProduct(nuevoProductoLocal)
        }
    }

    /**
     * Consume la API REST del proveedor externo para obtener su catálogo.
     */
    fun descargarCatalogoProveedor() {
        // 1. Iniciamos una corrutina.
        // Nota: Dispatchers.IO es la capa de la Disponibilidad para que no se congele la pantalla.
        viewModelScope.launch(Dispatchers.IO) {
            try {

                // 2. Aquí ocurre la magia. Retrofit va a internet, descarga el JSON,
                // Moshi lo convierte a una Lista de objetos de Kotlin, y nosotros
                // lo guardamos en la variable 'catalogoTraidoDeInternet'.
                // ¡Esto es una 'suspend function', por lo que la corrutina espera aquí a que el server responda!
                val catalogoTraidoDeInternet = apiService.getProductos()

                // 3. Empujamos la lista descargada a nuestro StateFlow.
                // Inmediatamente, la Interfaz Gráfica (Compose) se va a redibujar mostrando los datos.
                _catalogoExterno.value = catalogoTraidoDeInternet

                // Limpiamos errores si fue exitoso
                _mensajeErrorRed.value = null
            } catch (e: Exception) {
                // 4. Manejo de Errores (Integridad/Resiliencia)
                // ¿Qué pasa si no hay internet o el servidor está caído (Error 500)?
                // Retrofit lanzará una Exception. Debemos capturarla para que la app no haga "Crash".
                e.printStackTrace()
                _mensajeErrorRed.value = "Error al conectar con el servidor: ${e.localizedMessage}"
            }
        }
    }
}