package com.prorganics.prodelect.data.remote.api

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * Define los "Endpoints" (rutas) de tu servidor web.
 */
interface ProductApiService {
    //1.- OBTENER TODOS LOS PRODUCTOS
    // Simula una llamada a: https://mi-servidor.com/api/productos
    @GET("api/productos")
    // suspend es una corrutina.
    // Es vital para la Disponibilidad.
    //la app no se congelará mientras espera la respuesta de servidor.
    suspend fun getProductos(): List<ProductDto>


    //2.- OBTENER UN SOLO PRODUCTO
    // Simula: https://mi-servidor.com/api/productos/12345
    @GET("api/productos/{id}")
    suspend fun obtenerDetalleProducto(@Path("id") productId: String): ProductDto

    //3.- CREA UN NUEVO PRODUCTO
    @POST("api/productos")
    suspend fun crearNuevoProducto(@Body product: ProductDto): ProductDto

    //4.- ACTUALIZA UN PRODUCTO
    @PUT("api/productos/{id}")
    suspend fun actualizarProducto(@Path("id") productId: String, @Body product: ProductDto): ProductDto

    //5.- ELIMINA UN PRODUCTO
    @DELETE("api/productos/{id}")
    suspend fun eliminarProducto(@Path("id") productId: String): ProductDto
}

