
package com.prorganics.prodelect

import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.SearchView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.prorganics.prodelect.data.local.entity.ProductEntity
import com.prorganics.prodelect.sync.SyncManager
import com.prorganics.prodelect.ui.viewmodel.ProductViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * Actividad principal de ProdElect.
 *
 * Integra la interfaz del equipo con:
 * - ProductViewModel
 * - Room / SQLite
 * - Lectura en tiempo real de Firestore
 * - Cola de sincronización pendiente
 * - Interoperabilidad JSON / REST
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var syncManager: SyncManager

    private val productViewModel: ProductViewModel by viewModels()

    private lateinit var productoAdapter: ProductoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        val recyclerView =
            findViewById<RecyclerView>(R.id.recyclerViewProductos)

        val fabAgregar =
            findViewById<FloatingActionButton>(R.id.fabAgregarProducto)

        val searchView =
            findViewById<SearchView>(R.id.searchViewProductos)

        // Configurar adaptador
        productoAdapter = ProductoAdapter(
            onEditar = { producto ->
                mostrarDialogoProducto(producto)
            },
            onEliminar = { producto ->
                confirmarEliminacion(producto)
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = productoAdapter

        // Observar productos almacenados en Room.
        // La lista local es la fuente de datos de esta pantalla.
        lifecycleScope.launch {
            productViewModel.productos.collect { productos ->
                productoAdapter.actualizarProductos(productos)
            }
        }

        // Conservar la observación de Firebase en tiempo real.
        // Solo registramos los cambios por ahora, sin reemplazar
        // los productos locales ni generar nuevas operaciones.
        lifecycleScope.launch {
            productViewModel.productosNube.collect { productosNube ->
                Log.d(
                    "MainActivity",
                    "Productos observados en Firebase: ${productosNube.size}"
                )
            }
        }

        // Conservar la descarga JSON / REST del equipo.
        // La URL del servicio debe configurarse correctamente
        // antes de validar la descarga.
        if (savedInstanceState == null) {
            productViewModel.descargarCatalogoProveedor()
        }

        // Agregar producto
        fabAgregar.setOnClickListener {
            mostrarDialogoProducto()
        }

        // Buscador de productos
        searchView.setOnQueryTextListener(
            object : SearchView.OnQueryTextListener {

                override fun onQueryTextSubmit(
                    query: String?
                ): Boolean {
                    productoAdapter.filtrar(query.orEmpty())
                    return true
                }

                override fun onQueryTextChange(
                    newText: String?
                ): Boolean {
                    productoAdapter.filtrar(newText.orEmpty())
                    return true
                }
            }
        )

        // No ejecutar automáticamente la sincronización
        // hasta validar el funcionamiento del proyecto.
    }

    /**
     * Formulario para agregar o editar productos.
     */
    private fun mostrarDialogoProducto(
        productoExistente: ProductEntity? = null
    ) {
        val dialogView = layoutInflater.inflate(
            R.layout.dialog_producto,
            null
        )

        val etNombre =
            dialogView.findViewById<EditText>(
                R.id.etNombreProducto
            )

        val etDescripcion =
            dialogView.findViewById<EditText>(
                R.id.etDescripcionProducto
            )

        val etCantidad =
            dialogView.findViewById<EditText>(
                R.id.etCantidadProducto
            )

        val etPrecio =
            dialogView.findViewById<EditText>(
                R.id.etPrecioProducto
            )

        val etProveedor =
            dialogView.findViewById<EditText>(
                R.id.etProveedorProducto
            )

        // Cargar producto al editar
        productoExistente?.let { producto ->
            etNombre.setText(producto.nombre)
            etDescripcion.setText(producto.descripcion)
            etCantidad.setText(producto.cantidad.toString())
            etPrecio.setText(producto.precio.toString())
            etProveedor.setText(producto.proveedor)
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle(
                if (productoExistente == null)
                    "Agregar producto"
                else
                    "Editar producto"
            )
            .setView(dialogView)
            .setPositiveButton("Guardar", null)
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.setOnShowListener {

            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener {

                    val nombre =
                        etNombre.text.toString().trim()

                    val descripcion =
                        etDescripcion.text.toString().trim()

                    val cantidad =
                        etCantidad.text.toString().toIntOrNull()

                    val precio =
                        etPrecio.text.toString().toDoubleOrNull()

                    val proveedor =
                        etProveedor.text.toString().trim()

                    // Validaciones
                    if (
                        nombre.isBlank() ||
                        descripcion.isBlank() ||
                        proveedor.isBlank() ||
                        cantidad == null ||
                        precio == null ||
                        !precio.isFinite()
                    ) {
                        Toast.makeText(
                            this,
                            "Completa correctamente todos los campos",
                            Toast.LENGTH_SHORT
                        ).show()

                        return@setOnClickListener
                    }

                    if (cantidad < 0 || precio < 0) {
                        Toast.makeText(
                            this,
                            "Cantidad y precio no pueden ser negativos",
                            Toast.LENGTH_SHORT
                        ).show()

                        return@setOnClickListener
                    }

                    val id = productoExistente?.id
                        ?: UUID.randomUUID().toString()

                    // Guardar mediante el ViewModel.
                    // Este registra la operación en Room.
                    productViewModel.saveProduct(
                        id = id,
                        name = nombre,
                        description = descripcion,
                        qty = cantidad,
                        price = precio,
                        supplier = proveedor
                    )

                    Toast.makeText(
                        this@MainActivity,
                        if (productoExistente == null)
                            "Guardado solicitado"
                        else
                            "Actualización solicitada",
                        Toast.LENGTH_SHORT
                    ).show()

                    dialog.dismiss()
                }
        }

        dialog.show()
    }

    /**
     * Confirmar eliminación.
     */
    private fun confirmarEliminacion(
        producto: ProductEntity
    ) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar producto")
            .setMessage(
                "¿Deseas eliminar ${producto.nombre}?"
            )
            .setPositiveButton("Eliminar") { _, _ ->

                // El ViewModel registra DELETE en Room.
                productViewModel.deleteProduct(producto.id)

                Toast.makeText(
                    this@MainActivity,
                    "Eliminación solicitada",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
