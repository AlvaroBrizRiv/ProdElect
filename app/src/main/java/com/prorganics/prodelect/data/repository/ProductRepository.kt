
package com.prorganics.prodelect.data.repository

import androidx.room.withTransaction
import com.prorganics.prodelect.data.local.AppDatabase
import com.prorganics.prodelect.data.local.dao.ProductoDao
import com.prorganics.prodelect.data.local.dao.PendingSyncDao
import com.prorganics.prodelect.data.local.entity.ProductEntity
import com.prorganics.prodelect.data.local.entity.PendingSyncEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * Repositorio de productos de ProdElect.
 *
 * Conserva las operaciones locales de Room, la cola
 * de sincronización y los métodos de administración
 * de productos incorporados por el equipo.
 */
class ProductRepository(
    private val productoDao: ProductoDao,
    private val pendingSyncDao: PendingSyncDao,
    private val database: AppDatabase
) {

    // Consultar productos locales
    fun getAllProducts(): Flow<List<ProductEntity>> {
        return productoDao.getAllProducts()
            .flowOn(Dispatchers.IO)
    }

    // Consultar producto por ID
    suspend fun getProductById(id: String): ProductEntity? {
        return withContext(Dispatchers.IO) {
            productoDao.getProductById(id)
        }
    }

    // Crear producto y registrar operación pendiente
    suspend fun insertProduct(product: ProductEntity) {
        withContext(Dispatchers.IO) {
            database.withTransaction {
                productoDao.insertProduct(product)

                pendingSyncDao.insertPending(
                    PendingSyncEntity(
                        productId = product.id,
                        operation = "CREATE"
                    )
                )
            }
        }
    }

    // Insertar múltiples productos.
    // Se conserva el método incorporado por el equipo.
    // No genera eventos de subida a Firebase.
    suspend fun insertProducts(products: List<ProductEntity>) {
        withContext(Dispatchers.IO) {
            productoDao.insertProducts(products)
        }
    }

    // Actualizar producto y registrar operación pendiente
    suspend fun updateProduct(product: ProductEntity) {
        withContext(Dispatchers.IO) {
            database.withTransaction {
                productoDao.updateProduct(product)

                pendingSyncDao.insertPending(
                    PendingSyncEntity(
                        productId = product.id,
                        operation = "UPDATE"
                    )
                )
            }
        }
    }

    // Eliminar producto y registrar operación pendiente
    suspend fun deleteProduct(product: ProductEntity) {
        withContext(Dispatchers.IO) {
            database.withTransaction {
                productoDao.deleteProduct(product)

                pendingSyncDao.insertPending(
                    PendingSyncEntity(
                        productId = product.id,
                        operation = "DELETE"
                    )
                )
            }
        }
    }

    // Eliminar todos los productos locales.
    // Operación administrativa peligrosa:
    // no usar en pruebas con datos importantes.
    suspend fun deleteAllProducts() {
        withContext(Dispatchers.IO) {
            productoDao.deleteAllProducts()
        }
    }
}
