
package com.prorganics.prodelect

import android.os.Bundle
import android.widget.EditText
import android.widget.SearchView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.prorganics.prodelect.data.local.entity.ProductEntity
import com.prorganics.prodelect.ui.viewmodel.ProductViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.UUID
import androidx.activity.viewModels

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private lateinit var productoAdapter: ProductoAdapter
    
    // Inyectamos el ProductViewModel utilizando el delegado `by viewModels()` (Hilt)
    private val productViewModel: ProductViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Implementación Temporal ELIMINAR LUEGO.
        productViewModel.descargarCatalogoProveedor()

        // Interfaz principal XML
        setContentView(R.layout.activity_main)

        // Se eliminó la llamada a prepopulateDatabase() para evitar que elimine los productos
        // que creas manualmente al girar la pantalla o iniciar la Actividad.

        val recyclerView =
            findViewById<RecyclerView>(R.id.recyclerViewProductos)

        val fabAgregar =
            findViewById<FloatingActionButton>(R.id.fabAgregarProducto)

        val searchView =
            findViewById<SearchView>(R.id.searchViewProductos)

        // Configurar adaptador del RecyclerView
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

        // Observar cambios en la Nube de Firebase en tiempo real
        lifecycleScope.launch {
            productViewModel.productosNube.collect { productosFirebase ->
                val listaMapeada = productosFirebase.map { firebaseProd ->
                    ProductEntity(
                        id = firebaseProd.id.ifEmpty { UUID.randomUUID().toString() },
                        nombre = firebaseProd.nombre,
                        descripcion = firebaseProd.descripcion,
                        cantidad = firebaseProd.cantidad,
                        precio = firebaseProd.precio,
                        proveedor = firebaseProd.proveedor
                    )
                }
                productoAdapter.actualizarProductos(listaMapeada)
            }
        }

        // Botón para agregar productos
        fabAgregar.setOnClickListener {
            mostrarDialogoProducto()
        }

        // Buscador de productos en tiempo real
        searchView.setOnQueryTextListener(
            object : SearchView.OnQueryTextListener {

                override fun onQueryTextSubmit(query: String?): Boolean {
                    productoAdapter.filtrar(query.orEmpty())
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    productoAdapter.filtrar(newText.orEmpty())
                    return true
                }
            }
        )
    }

    // Diálogo para agregar o editar productos
    private fun mostrarDialogoProducto(
        productoExistente: ProductEntity? = null
    ) {

        val dialogView = layoutInflater.inflate(
            R.layout.dialog_producto,
            null
        )

        val etNombre =
            dialogView.findViewById<EditText>(R.id.etNombreProducto)

        val etDescripcion =
            dialogView.findViewById<EditText>(R.id.etDescripcionProducto)

        val etCantidad =
            dialogView.findViewById<EditText>(R.id.etCantidadProducto)

        val etPrecio =
            dialogView.findViewById<EditText>(R.id.etPrecioProducto)

        val etProveedor =
            dialogView.findViewById<EditText>(R.id.etProveedorProducto)

        // Cargar datos del producto existente
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

                    // Validar campos obligatorios
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

                    // Validar cantidad y precio
                    if (cantidad < 0 || precio < 0) {

                        Toast.makeText(
                            this,
                            "Cantidad y precio no pueden ser negativos",
                            Toast.LENGTH_SHORT
                        ).show()

                        return@setOnClickListener
                    }

                    val id = productoExistente?.id ?: UUID.randomUUID().toString()

                    // Delega el guardado al ViewModel, que maneja tanto Room como Firebase.
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
                        if (productoExistente == null) "Producto agregado" else "Producto actualizado",
                        Toast.LENGTH_SHORT
                    ).show()

                    dialog.dismiss()
                }
        }

        dialog.show()
    }

    // Confirmación antes de eliminar un producto
    private fun confirmarEliminacion(
        producto: ProductEntity
    ) {

        AlertDialog.Builder(this)
            .setTitle("Eliminar producto")
            .setMessage("¿Deseas eliminar ${producto.nombre}?")
            .setPositiveButton("Eliminar") { _, _ ->

                // Delega la eliminación al ViewModel, que borra tanto en Room como en Firebase.
                productViewModel.deleteProduct(producto.id)

                Toast.makeText(
                    this@MainActivity,
                    "Producto eliminado",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
