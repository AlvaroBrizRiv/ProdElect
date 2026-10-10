package com.prorganics.prodelect.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prorganics.prodelect.data.local.entity.ProductEntity
import com.prorganics.prodelect.data.repository.ProductRepository
import com.prorganics.prodelect.data.repository.FirebaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val firebaseRepository: FirebaseRepository
) : ViewModel() {

    /**
     * Flujo de estado persistente (StateFlow) que contiene la lista de inventario.
     * 
     * Transforma el 'Flow' reactivo de la base de datos a un 'StateFlow' seguro para Compose.
     * - `stateIn` convierte el Flow frío del Repositorio a un StateFlow caliente.
     * - `SharingStarted.WhileSubscribed(5000)`: Optimización de memoria; suspende la consulta a la BD 
     *   si la app se va a segundo plano por más de 5 segundos.
     */
    val productos: StateFlow<List<ProductEntity>> = repository.getAllProducts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Evento UI: Añadir un producto nuevo al inventario local.
     */
    fun addProduct(name: String, description: String, qty: Int, price: Double, supplier: String) {
        val newProduct = ProductEntity(
            nombre = name,
            descripcion = description,
            cantidad = qty,
            precio = price,
            proveedor = supplier
        )
        
        // Lanzamos la corrutina en el scope del ViewModel. Se destruye automáticamente si la UI se cierra.
        viewModelScope.launch {
            repository.insertProduct(newProduct)
        }
    }

    /**
     * Evento UI: Eliminar un producto.
     */
    fun deleteProduct(product: ProductEntity) {
        viewModelScope.launch {
            repository.deleteProduct(product)
        }
    }
}