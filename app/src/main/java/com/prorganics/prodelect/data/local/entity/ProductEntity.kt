package com.prorganics.prodelect.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Entidad que representa la tabla 'products' en la base de datos local.
 * Se utiliza UUID para los IDs para garantizar la unicidad (Integridad)
 * y ofuscar la cantidad de registros frente a posibles ataques de enumeración (Confidencialidad).
 */
@Entity(tableName = "products")
data class ProductEntity(
    // ID único utilizando UUID por defecto.
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "name")
    val nombre: String,

    @ColumnInfo(name = "description")
    val descripcion: String,

    // Se recomienda no aceptar nulos aquí para asegurar la Integridad de la cantidad y el precio.
    @ColumnInfo(name = "quantity")
    val cantidad: Int,

    @ColumnInfo(name = "price")
    val precio: Double,

    @ColumnInfo(name = "supplier")
    val proveedor: String
)
