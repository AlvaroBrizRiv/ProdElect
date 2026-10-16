
package com.prorganics.prodelect.di

import android.content.Context
import com.prorganics.prodelect.data.local.AppDatabase
import com.prorganics.prodelect.data.local.dao.ProductoDao
import com.prorganics.prodelect.data.local.dao.PendingSyncDao
import com.prorganics.prodelect.data.remote.api.ProductApiService
import com.prorganics.prodelect.data.repository.FirebaseRepository
import com.prorganics.prodelect.data.repository.ProductRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Singleton

/**
 * Módulo de dependencias de ProdElect.
 *
 * Integra:
 * - Room / SQLite
 * - Cola de operaciones pendientes
 * - Repositorios locales y Firebase
 * - Retrofit / Moshi para JSON y REST
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // Base de datos local Room
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    // DAO de productos
    @Provides
    fun provideProductoDao(
        database: AppDatabase
    ): ProductoDao {
        return database.productoDao()
    }

    // DAO de operaciones pendientes
    @Provides
    fun providePendingSyncDao(
        database: AppDatabase
    ): PendingSyncDao {
        return database.pendingSyncDao()
    }

    // Repositorio de productos con persistencia local
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

    // Repositorio remoto de Firebase
    @Provides
    @Singleton
    fun provideFirebaseRepository(): FirebaseRepository {
        return FirebaseRepository()
    }

    // Retrofit para interoperabilidad JSON / REST
    @Provides
    @Singleton
    fun provideRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://mi-servidor-falso.com/")
            .addConverterFactory(
                MoshiConverterFactory.create()
            )
            .build()
    }

    // Servicio REST utilizado por ProductViewModel
    @Provides
    @Singleton
    fun provideProductApiService(
        retrofit: Retrofit
    ): ProductApiService {
        return retrofit.create(
            ProductApiService::class.java
        )
    }
}
