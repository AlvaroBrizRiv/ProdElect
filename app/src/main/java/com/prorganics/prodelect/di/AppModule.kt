
package com.prorganics.prodelect.di

import android.content.Context

import com.prorganics.prodelect.data.local.AppDatabase
import com.prorganics.prodelect.data.local.dao.ProductoDao
import com.prorganics.prodelect.data.local.dao.PendingSyncDao
import com.prorganics.prodelect.data.repository.ProductRepository
import com.prorganics.prodelect.data.repository.FirebaseRepository

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

import javax.inject.Singleton

/**
 * Modulo de inyeccion de dependencias de ProdElect.
 *
 * Proporciona la base de datos Room, los DAO
 * y los repositorios de datos locales y remotos.
 *
 * Incluye soporte para la cola de operaciones
 * pendientes de sincronizacion.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * Proporciona una instancia unica de Room.
     *
     * Utiliza AppDatabase.getDatabase() para
     * aplicar la migracion de la version 1 a la 2.
     */
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    /**
     * Proporciona el DAO de productos locales.
     */
    @Provides
    fun provideProductoDao(
        database: AppDatabase
    ): ProductoDao {
        return database.productoDao()
    }

    /**
     * Proporciona el DAO de operaciones pendientes.
     */
    @Provides
    fun providePendingSyncDao(
        database: AppDatabase
    ): PendingSyncDao {
        return database.pendingSyncDao()
    }

    /**
     * Proporciona el repositorio local de productos.
     */

    @Provides
    @Singleton
    fun provideProductRepository(
        productoDao: ProductoDao,
        pendingSyncDao: PendingSyncDao,
        database: AppDatabase
    ): ProductRepository {
        return ProductRepository(
            productoDao,
            pendingSyncDao,
            database
        )
    }

    /**
     * Proporciona el repositorio de Firebase.
     */
    @Provides
    @Singleton
    fun provideFirebaseRepository(): FirebaseRepository {
        return FirebaseRepository()
    }
}
