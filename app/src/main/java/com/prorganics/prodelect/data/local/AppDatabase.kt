
package com.prorganics.prodelect.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.prorganics.prodelect.data.local.dao.ProductoDao
import com.prorganics.prodelect.data.local.dao.PendingSyncDao
import com.prorganics.prodelect.data.local.entity.ProductEntity
import com.prorganics.prodelect.data.local.entity.PendingSyncEntity

/**
 * Base de datos local de ProdElect.
 *
 * Versión 2:
 * Incorpora la tabla pending_sync para almacenar
 * operaciones pendientes de sincronización.
 */
@Database(
    entities = [
        ProductEntity::class,
        PendingSyncEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun productoDao(): ProductoDao

    abstract fun pendingSyncDao(): PendingSyncDao

    companion object {

        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Migración de la versión 1 a la versión 2.
         *
         * Crea la tabla de operaciones pendientes
         * sin eliminar los productos existentes.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `pending_sync` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `productId` TEXT NOT NULL,
                        `operation` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "prodelect_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
