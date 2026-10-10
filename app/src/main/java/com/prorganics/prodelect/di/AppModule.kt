package com.prorganics.prodelect.di

import android.content.Context
import androidx.room.Room
import com.prorganics.prodelect.data.local.AppDatabase
import com.prorganics.prodelect.data.local.dao.ProductoDao
import com.prorganics.prodelect.data.repository.FirebaseRepository
import com.prorganics.prodelect.data.repository.ProductRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Módulo de Inyección de Dependencias de Hilt.
 *
 * InstallIn(SingletonComponent::class) significa que las dependencias
 * aquí provistas vivirán el mismo tiempo que la aplicación (Singleton).
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * Provee la instancia Singleton de la Base de Datos de Room.
     */
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "prodelect_database"
        )
        // .fallbackToDestructiveMigration() // Opcional: útil durante el desarrollo inicial
        .build()
    }

    /**
     * Provee el DAO de Productos, requerido por el repositorio local.
     */
    @Provides
    fun provideProductoDao(database: AppDatabase): ProductoDao {
        return database.productoDao()
    }

    /**
     * Provee el Repositorio de la Base de Datos Local.
     */
    @Provides
    @Singleton
    fun provideProductRepository(productoDao: ProductoDao): ProductRepository {
        return ProductRepository(productoDao)
    }

    /**
     * Provee el Repositorio de Firebase.
     */
    @Provides
    @Singleton
    fun provideFirebaseRepository(): FirebaseRepository {
        return FirebaseRepository()
    }
}