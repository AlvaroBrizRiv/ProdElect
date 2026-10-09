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
import com.prorganics.prodelect.data.local.AppDatabase
import com.prorganics.prodelect.data.local.dao.ProductoDao
import com.prorganics.prodelect.data.local.entity.ProductEntity
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var productoAdapter: ProductoAdapter
    private lateinit var productoDao: ProductoDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val recyclerView =
            findViewById<RecyclerView>(R.id.recyclerViewProductos)

        val fabAgregar =
            findViewById<FloatingActionButton>(R.id.fabAgregarProducto)

        val searchView =
            findViewById<SearchView>(R.id.searchViewProductos)

        // Base de datos local Room
        val database = AppDatabase.getDatabase(this)
        productoDao = database.productoDao()

        // Configuración del RecyclerView
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

        // Room Flow mantiene la lista actualizada automáticamente
        lifecycleScope.launch {
            productoDao.getAllProducts().collect { productos ->
                productoAdapter.actualizarProductos(productos)
            }
        }

        // Agregar nuevo producto
        fabAgregar.setOnClickListener {
            mostrarDialogoProducto()
        }

        // Búsqueda en tiempo real
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

        // Cargar datos cuando se está editando
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

                    // Validar campos
                    if (
                        nombre.isBlank() ||
                        descripcion.isBlank() ||
                        proveedor.isBlank() ||
                        cantidad == null ||
                        precio == null
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

                    val producto = ProductEntity(
                        id = productoExistente?.id
                            ?: java.util.UUID.randomUUID().toString(),
                        nombre = nombre,
                        descripcion = descripcion,
                        cantidad = cantidad,
                        precio = precio,
                        proveedor = proveedor
                    )

                    lifecycleScope.launch {

                        if (productoExistente == null) {
                            productoDao.insertProduct(producto)

                            Toast.makeText(
                                this@MainActivity,
                                "Producto agregado",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            productoDao.updateProduct(producto)

                            Toast.makeText(
                                this@MainActivity,
                                "Producto actualizado",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    dialog.dismiss()
                }
        }

        dialog.show()
    }

    private fun confirmarEliminacion(
        producto: ProductEntity
    ) {

        AlertDialog.Builder(this)
            .setTitle("Eliminar producto")
            .setMessage("¿Deseas eliminar ${producto.nombre}?")
            .setPositiveButton("Eliminar") { _, _ ->

                lifecycleScope.launch {
                    productoDao.deleteProduct(producto)

                    Toast.makeText(
                        this@MainActivity,
                        "Producto eliminado",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
