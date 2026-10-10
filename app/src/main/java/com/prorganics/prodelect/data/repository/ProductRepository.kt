package com.prorganics.prodelect.data.repository

import com.prorganics.prodelect.data.local.dao.ProductoDao
import com.prorganics.prodelect.data.local.entity.ProductEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * Repositorio Local para la gestión del inventario de productos.
 *
 * Actúa como intermediario entre las fuentes de datos (Room en este caso) y la lógica de presentación.
 * 
 * Principios aplicados:
 * - Single Source of Truth: La base de datos local dicta el estado reactivo de la UI.
 * - Disponibilidad (CID): Se utiliza `withContext(Dispatchers.IO)` para asegurar que las operaciones de lectura/escritura
 *   no bloqueen el hilo principal (Main Thread), manteniendo la app responsiva.
 * - Escalabilidad: El acoplamiento a través de interfaces (DAO) permite que cambiar la lógica subyacente sea sencillo.
 */
class ProductRepository(private val productoDao: ProductoDao) {

    /**
     * Obtiene el flujo (Flow) de productos en tiempo real.
     * Cualquier cambio en la base de datos (Insert, Update, Delete) emitirá 
     * automáticamente una nueva lista actualizada hacia la interfaz (Jetpack Compose).
     */
    fun getAllProducts(): Flow<List<ProductEntity>> {
        // flowOn(Dispatchers.IO) asegura que las emisiones de datos y consultas ocurran en el hilo de IO.
        // Room ya es asíncrono con Flow, pero esto garantiza la seguridad extra de ejecución.
        return productoDao.getAllProducts().flowOn(Dispatchers.IO)
    }

    /**
     * Consulta segura de un producto específico por su ID.
     */
    suspend fun getProductById(id: String): ProductEntity? {
        return withContext(Dispatchers.IO) {
            productoDao.getProductById(id)
        }
    }

    /**
     * Inserta un nuevo producto de forma asíncrona asegurando Integridad de los datos.
     */
    suspend fun insertProduct(product: ProductEntity) {
        withContext(Dispatchers.IO) {
            productoDao.insertProduct(product)
        }
    }

    /**
     * Actualiza la información de un producto existente.
     */
    suspend fun updateProduct(product: ProductEntity) {
        withContext(Dispatchers.IO) {
            productoDao.updateProduct(product)
        }
    }

    /**
     * Elimina un producto de la base de datos.
     */
    suspend fun deleteProduct(product: ProductEntity) {
        withContext(Dispatchers.IO) {
            productoDao.deleteProduct(product)
        }
    }
}