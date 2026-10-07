package com.prorganics.prodelect.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.prorganics.prodelect.data.local.entity.ProductEntity
import kotlinx.coroutines.flow.Flow

/**
 * Interfaz de Acceso a Datos (DAO) para los productos.
 * Utiliza 'suspend functions' para asegurar que las operaciones se ejecuten fuera del
 * hilo principal, previniendo bloqueos de UI (Disponibilidad).
 */
@Dao
interface ProductoDao {

    /**
     * Inserta un nuevo producto. 
     * Se usa OnConflictStrategy.REPLACE para evitar fallos si un ID coincide (Integridad).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)

    /**
     * Consulta todos los productos.
     * Retorna un Flow de Kotlin para proveer actualizaciones reactivas en tiempo real.
     */
    @Query("SELECT * FROM products")
    fun getAllProducts(): Flow<List<ProductEntity>>

    /**
     * Consulta segura de un producto específico por su ID.
     */
    @Query("SELECT * FROM products WHERE id = :productId LIMIT 1")
    suspend fun getProductById(productId: String): ProductEntity?

    /**
     * Actualiza un producto existente en la base de datos.
     */
    @Update
    suspend fun updateProduct(product: ProductEntity)

    /**
     * Elimina el producto proporcionado.
     */
    @Delete
    suspend fun deleteProduct(product: ProductEntity)
}
