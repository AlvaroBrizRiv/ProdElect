
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
 * Repositorio local de productos.
 *
 * Registra las operaciones CRUD en Room y
 * guarda una operación pendiente de sincronización.
 */
class ProductRepository(
    private val productoDao: ProductoDao,
    private val pendingSyncDao: PendingSyncDao,
    private val database: AppDatabase
) {

    fun getAllProducts(): Flow<List<ProductEntity>> {
        return productoDao.getAllProducts()
            .flowOn(Dispatchers.IO)
    }

    suspend fun getProductById(id: String): ProductEntity? {
        return withContext(Dispatchers.IO) {
            productoDao.getProductById(id)
        }
    }

    /**
     * Inserta un producto y registra su sincronización pendiente.
     */
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

    /**
     * Actualiza un producto y registra su sincronización pendiente.
     */
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

    /**
     * Elimina un producto localmente y registra la operación.
     */
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
}
