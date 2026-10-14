package com.prorganics.prodelect.data.remote.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * DTO (Data Transfer Object). Su única responsabilidad es recibir los datos de la red.
 * Mantenerlo separado de ProductEntity (Room) protege la Integridad de tu base de datos local.
 */
@JsonClass(generateAdapter = true) // Le pide a Moshi que escriba código sin usar "reflexión".
data class ProductDto(
    @Json(name = "id_producto") // Así se llama el campo en el JSON del servidor web
    val id: String, // Aquí se le pone un nuevo nombre.

    @Json(name = "nombre_prod")
    val nombre: String,

    @Json(name = "desc_prod")
    val descripcion: String,

    @Json(name = "stock_disp")
    val cantidad: Int,

    @Json(name = "precio_actual")
    val precio: Double,

    @Json(name = "Proveedor_asociado")
    val proveedor: String,
)

