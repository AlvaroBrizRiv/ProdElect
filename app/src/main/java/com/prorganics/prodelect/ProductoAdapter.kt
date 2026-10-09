package com.prorganics.prodelect

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.prorganics.prodelect.data.local.entity.ProductEntity

class ProductoAdapter(
    private var productos: List<ProductEntity> = emptyList(),
    private val onEditar: (ProductEntity) -> Unit,
    private val onEliminar: (ProductEntity) -> Unit
) : RecyclerView.Adapter<ProductoAdapter.ProductoViewHolder>() {

    // Lista completa proveniente de Room
    private var productosCompletos: List<ProductEntity> = emptyList()

    class ProductoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nombre: TextView = view.findViewById(R.id.tvNombreProducto)
        val descripcion: TextView = view.findViewById(R.id.tvDescripcionProducto)
        val cantidad: TextView = view.findViewById(R.id.tvCantidadProducto)
        val precio: TextView = view.findViewById(R.id.tvPrecioProducto)
        val proveedor: TextView = view.findViewById(R.id.tvProveedorProducto)
        val btnEditar: Button = view.findViewById(R.id.btnEditarProducto)
        val btnEliminar: Button = view.findViewById(R.id.btnEliminarProducto)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ProductoViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_producto, parent, false)

        return ProductoViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ProductoViewHolder,
        position: Int
    ) {
        val producto = productos[position]

        holder.nombre.text = producto.nombre
        holder.descripcion.text = producto.descripcion
        holder.cantidad.text = "Cantidad: ${producto.cantidad}"
        holder.precio.text = "Precio: $${producto.precio}"
        holder.proveedor.text = "Proveedor: ${producto.proveedor}"

        holder.btnEditar.setOnClickListener {
            onEditar(producto)
        }

        holder.btnEliminar.setOnClickListener {
            onEliminar(producto)
        }
    }

    override fun getItemCount(): Int = productos.size

    // Recibe los productos actualizados desde Room
    fun actualizarProductos(nuevaLista: List<ProductEntity>) {
        productosCompletos = nuevaLista
        productos = nuevaLista
        notifyDataSetChanged()
    }

    // Filtra productos mientras el usuario escribe
    fun filtrar(texto: String) {

        productos = if (texto.isBlank()) {
            productosCompletos
        } else {
            productosCompletos.filter { producto ->
                producto.nombre.contains(texto, ignoreCase = true) ||
                        producto.descripcion.contains(texto, ignoreCase = true) ||
                        producto.proveedor.contains(texto, ignoreCase = true)
            }
        }

        notifyDataSetChanged()
    }
}