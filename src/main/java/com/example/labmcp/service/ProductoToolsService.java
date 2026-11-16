package com.example.labmcp.service;

import com.example.labmcp.model.Producto;
import com.example.labmcp.repository.ProductoRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * CRUD de Productos expuesto como herramientas MCP.
 *
 * Cada método @Tool se publica automáticamente por el servidor MCP:
 *   - nombre      = nombre del método
 *   - descripción = atributo description
 *   - JSON schema      = derivado de los parámetros (incluye @ToolParam)
 *
 * Claude (desde Claude Desktop) podrá invocar estos métodos cuando el usuario
 * hable en lenguaje natural:
 */
@Component
public class ProductoToolsService {

    private final ProductoRepository repo;

    public ProductoToolsService(ProductoRepository repo) {
        this.repo = repo;
    }

    @Tool(name = "registrarProducto", description = "Agrega un producto al catálogo del supermercado.")
    @Transactional
    public Producto registrarProducto(
            @ToolParam(description = "Nombre del producto") String nombre,
            @ToolParam(description = "Descripción del producto") String descripcion,
            @ToolParam(description = "Precio unitario entero, no negativo") Integer precio) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del producto es obligatorio.");
        }
        if (precio == null || precio < 0) {
            throw new IllegalArgumentException("El precio debe ser un valor entero no negativo.");
        }
        return repo.save(new Producto(nombre.trim(),
                descripcion == null || descripcion.isBlank() ? null : descripcion.trim(),
                precio));
    }

    // ---------- READ ----------

    @Tool(name = "listProductos", description = "Devuelve la lista completa de Productos almacenados.")
    @Transactional(readOnly = true)
    public List<Producto> listarProductos() {
        return repo.findAll();
    }

    @Tool(name = "buscarProductos", description = "Busca productos cuyo nombre contiene el fragmento indicado (sin distinguir mayúsculas).")
    @Transactional(readOnly = true)
    public List<Producto> buscarProductos(
            @ToolParam(description = "Fragmento de texto a buscar en el nombre del producto") String fragmento) {
        if (fragmento == null || fragmento.isBlank()) {
            throw new IllegalArgumentException("El texto de búsqueda no puede estar vacío.");
        }
        return repo.findByNombreContainingIgnoreCase(fragmento.trim());
    }
}