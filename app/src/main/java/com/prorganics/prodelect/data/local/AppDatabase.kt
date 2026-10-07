package com.prorganics.prodelect.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.prorganics.prodelect.data.local.dao.ProductoDao
import com.prorganics.prodelect.data.local.entity.ProductEntity

/**
 * Configuración principal de la Base de Datos Room.
 * Almacena las entidades y la versión para mantener un control estricto de las migraciones
 * y la estructura (Escalabilidad e Integridad).
 */
@Database(
    entities = [ProductEntity::class],
    version = 1,
    exportSchema = false // Oculta detalles del esquema en compilaciones, añadiendo privacidad
)
abstract class AppDatabase : RoomDatabase() {

    // Exponemos el DAO para poder realizar operaciones
    abstract fun productoDao(): ProductoDao

    companion object {
        // Asegura que los cambios en INSTANCE sean inmediatamente visibles en todos los hilos
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Obtiene la instancia única de la base de datos utilizando el patrón Singleton.
         * Centralizar el acceso maximiza el rendimiento y previene conflictos (Disponibilidad).
         */
        fun getDatabase(context: Context): AppDatabase {
            // Retorna la instancia existente, o bien la crea sincronizadamente
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "prodelect_database"
                )
                // Nota de seguridad: Para producción, se puede integrar SQLCipher aquí
                // si los productos contienen información sensible (Confidencialidad).
                .build()
                
                INSTANCE = instance
                instance
            }
        }
    }
}
