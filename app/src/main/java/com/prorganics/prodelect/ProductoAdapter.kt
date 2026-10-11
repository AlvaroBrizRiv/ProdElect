
package com.prorganics.prodelect

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.prorganics.prodelect.data.local.entity.ProductEntity
import java.text.NumberFormat
import java.util.Locale

class ProductoAdapter(
    private var productos: List<ProductEntity> = emptyList(),
    private val onEditar: (ProductEntity) -> Unit,
    private val onEliminar: (ProductEntity) -> Unit
) : RecyclerView.Adapter<ProductoAdapter.ProductoViewHolder>(), Filterable {

    private var productosCompletos: List<ProductEntity> = productos.toList()
    private var textoBusqueda: String = ""

    private val formatoPrecio = NumberFormat.getCurrencyInstance(
        Locale.forLanguageTag("es-CL")
    ).apply {
        maximumFractionDigits = 0
        minimumFractionDigits = 0
    }

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
        holder.precio.text = "Precio: ${formatoPrecio.format(producto.precio)}"
        holder.proveedor.text = "Proveedor: ${producto.proveedor}"

        holder.btnEditar.setOnClickListener {
            onEditar(producto)
        }

        holder.btnEliminar.setOnClickListener {
            onEliminar(producto)
        }
    }

    override fun getItemCount(): Int = productos.size

    // Actualizar listado cuando Room emite nuevos datos
    fun actualizarProductos(nuevaLista: List<ProductEntity>) {
        productosCompletos = nuevaLista.toList()
        aplicarFiltroActual()
    }

    // Mantener compatibilidad con MainActivity
    fun filtrar(texto: String) {
        filter.filter(texto)
    }

    // Implementación de Filterable
    override fun getFilter(): Filter {
        return object : Filter() {

            override fun performFiltering(
                constraint: CharSequence?
            ): FilterResults {

                val consulta = constraint
                    ?.toString()
                    ?.trim()
                    .orEmpty()

                val resultados = if (consulta.isBlank()) {
                    productosCompletos.toList()
                } else {
                    productosCompletos.filter { producto ->
                        producto.nombre.contains(consulta, ignoreCase = true) ||
                                producto.descripcion.contains(consulta, ignoreCase = true) ||
                                producto.proveedor.contains(consulta, ignoreCase = true)
                    }
                }

                return FilterResults().apply {
                    values = Pair(consulta, resultados)
                    count = resultados.size
                }
            }

            override fun publishResults(
                constraint: CharSequence?,
                results: FilterResults?
            ) {
                @Suppress("UNCHECKED_CAST")
                val resultado = results?.values
                        as? Pair<String, List<ProductEntity>>

                if (resultado != null) {
                    textoBusqueda = resultado.first
                    aplicarFiltroActual()
                }
            }
        }
    }

    // Aplicar filtro vigente, incluso cuando Room actualiza la lista
    private fun aplicarFiltroActual() {
        productos = if (textoBusqueda.isBlank()) {
            productosCompletos
        } else {
            productosCompletos.filter { producto ->
                producto.nombre.contains(textoBusqueda, ignoreCase = true) ||
                        producto.descripcion.contains(textoBusqueda, ignoreCase = true) ||
                        producto.proveedor.contains(textoBusqueda, ignoreCase = true)
            }
        }

        notifyDataSetChanged()
    }
}
