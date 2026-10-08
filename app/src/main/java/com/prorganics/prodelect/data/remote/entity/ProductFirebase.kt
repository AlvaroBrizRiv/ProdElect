package com.prorganics.prodelect.data.remote.entity

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties

/**
 * Entidad que representa la estructura de un 'Producto' en Firebase Firestore.
 * 
 * Principios aplicados:
 * - DTO / Firebase Data Class: Utiliza un constructor sin argumentos (valores por defecto) necesario para Firebase.
 * - @IgnoreExtraProperties: Evita bloqueos y errores de parsing si se añaden campos adicionales en la nube (escalabilidad).
 * - @DocumentId: Sirve para mapear el ID del documento en Firestore al campo local.
 */
@IgnoreExtraProperties
data class ProductFirebase(
    @DocumentId
    var id: String = "",
    
    var nombre: String = "",
    var descripcion: String = "",
    var cantidad: Int = 0,
    var precio: Double = 0.0,
    var proveedor: String = ""
) {
    // Constructor secundario para inicializar campos comunes de la base local y transformarlos a objeto de la nube.
    // Usar 'var' y valores por defecto evita excepciones de Deserialización de Firestore.
    
    /**
     * Mapeador o lógica adicional puede ser añadida aquí si es necesario convertir la Data Class
     * hacia o desde el Domain/Entity local.
     */
}