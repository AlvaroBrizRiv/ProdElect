
package com.prorganics.prodelect.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Observa la disponibilidad de internet.
 * Cuando Android confirma una red con acceso a internet,
 * solicita el envio de las operaciones pendientes.
 */
@Singleton
class NetworkSyncObserver @Inject constructor(
    @ApplicationContext private val context: Context,
    private val syncManager: SyncManager
) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE)
                as ConnectivityManager

    private var callback: ConnectivityManager.NetworkCallback? = null

    fun iniciar(scope: CoroutineScope) {
        if (callback != null) return

        val networkCallback =
            object : ConnectivityManager.NetworkCallback() {

                override fun onCapabilitiesChanged(
                    network: Network,
                    capabilities: NetworkCapabilities
                ) {
                    val tieneInternet =
                        capabilities.hasCapability(
                            NetworkCapabilities.NET_CAPABILITY_INTERNET
                        ) &&
                                capabilities.hasCapability(
                                    NetworkCapabilities.NET_CAPABILITY_VALIDATED
                                )

                    if (tieneInternet) {
                        Log.d(
                            "NetworkSyncObserver",
                            "Internet disponible. Sincronizando pendientes."
                        )

                        scope.launch {
                            try {
                                syncManager.sincronizarPendientes()
                            } catch (e: Exception) {
                                Log.e(
                                    "NetworkSyncObserver",
                                    "Error al sincronizar tras recuperar internet",
                                    e
                                )
                            }
                        }
                    }
                }
            }

        callback = networkCallback

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(
            request,
            networkCallback
        )
    }

    fun detener() {
        callback?.let {
            connectivityManager.unregisterNetworkCallback(it)
        }
        callback = null
    }
}
