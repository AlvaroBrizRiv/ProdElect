package com.prorganics.prodelect

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Clase Application principal del proyecto.
 * 
 * `@HiltAndroidApp` desencadena la generación de código de Hilt, 
 * incluyendo una clase base para la aplicación que sirve como el contenedor de dependencias
 * a nivel de la aplicación.
 */
@HiltAndroidApp
class ProdElectApp : Application()