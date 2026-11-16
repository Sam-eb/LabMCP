package com.example.labmcp.service;

import com.example.labmcp.model.Cliente;
import com.example.labmcp.model.DetallePedido;
import com.example.labmcp.model.EstadoPedido;
import com.example.labmcp.model.Pedido;
import com.example.labmcp.model.Producto;
import com.example.labmcp.repository.ClienteRepository;
import com.example.labmcp.repository.PedidoRepository;
import com.example.labmcp.repository.ProductoRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class PedidoToolsService {
    private final ClienteRepository clienteRepository;
    private final ProductoRepository productoRepository;
    private final PedidoRepository pedidoRepository;

    public PedidoToolsService(
            ClienteRepository clienteRepository,
            ProductoRepository productoRepository,
            PedidoRepository pedidoRepository) {
        this.clienteRepository = clienteRepository;
        this.productoRepository = productoRepository;
        this.pedidoRepository = pedidoRepository;
    }

    @Tool(name = "crearPedido", description = "Crea un pedido a domicilio con productos del catálogo. Las cantidades deben ser positivas.")
    @Transactional
    public PedidoResumen crearPedido(
            @ToolParam(description = "Identificador del cliente registrado") Long clienteId,
            @ToolParam(description = "Identificadores de productos; cada elemento corresponde a la cantidad en la misma posición") List<Long> productoIds,
            @ToolParam(description = "Cantidad solicitada para cada producto, en el mismo orden que productoIds") List<Integer> cantidades,
            @ToolParam(description = "Dirección donde se debe entregar el pedido") String direccionEntrega) {
        if (clienteId == null) {
            throw new IllegalArgumentException("El identificador del cliente es obligatorio.");
        }
        if (direccionEntrega == null || direccionEntrega.isBlank()) {
            throw new IllegalArgumentException("La dirección de entrega es obligatoria.");
        }
        if (productoIds == null || cantidades == null || productoIds.isEmpty()
                || productoIds.size() != cantidades.size()) {
            throw new IllegalArgumentException("Debe indicar al menos un producto y una cantidad para cada producto.");
        }

        Map<Long, Integer> cantidadesPorProducto = new LinkedHashMap<>();
        for (int i = 0; i < productoIds.size(); i++) {
            Long productoId = productoIds.get(i);
            Integer cantidad = cantidades.get(i);
            if (productoId == null || cantidad == null || cantidad <= 0) {
                throw new IllegalArgumentException("Los productos deben existir y sus cantidades deben ser positivas.");
            }
            cantidadesPorProducto.merge(productoId, cantidad, Math::addExact);
        }

        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el cliente indicado."));
        Map<Long, Producto> productos = new LinkedHashMap<>();
        for (Producto producto : productoRepository.findAllById(cantidadesPorProducto.keySet())) {
            productos.put(producto.getId(), producto);
        }

        Pedido pedido = new Pedido(cliente, direccionEntrega.trim(), Instant.now());
        long total = 0;
        List<LineaPedidoResumen> lineas = new ArrayList<>();
        for (Map.Entry<Long, Integer> entrada : cantidadesPorProducto.entrySet()) {
            Producto producto = productos.get(entrada.getKey());
            if (producto == null) {
                throw new IllegalArgumentException("No se encontró el producto con identificador " + entrada.getKey() + ".");
            }
            if (producto.getPrecio() < 0) {
                throw new IllegalStateException("El producto " + producto.getId() + " tiene un precio inválido.");
            }

            int cantidad = entrada.getValue();
            long subtotal = Math.multiplyExact((long) producto.getPrecio(), cantidad);
            total = Math.addExact(total, subtotal);
            pedido.agregarDetalle(new DetallePedido(producto, cantidad, producto.getPrecio(), subtotal));
            lineas.add(new LineaPedidoResumen(
                    producto.getId(), producto.getNombre(), cantidad, producto.getPrecio(), subtotal));
        }
        pedido.setTotal(total);

        Pedido pedidoGuardado = pedidoRepository.save(pedido);
        return resumir(pedidoGuardado, lineas);
    }

    @Tool(name = "listarPedidosCliente", description = "Lista los pedidos de un cliente, del más reciente al más antiguo.")
    @Transactional(readOnly = true)
    public List<PedidoResumen> listarPedidosCliente(
            @ToolParam(description = "Identificador del cliente") Long clienteId) {
        if (clienteId == null || !clienteRepository.existsById(clienteId)) {
            throw new IllegalArgumentException("No se encontró el cliente indicado.");
        }
        return pedidoRepository.findByClienteIdOrderByFechaCreacionDesc(clienteId)
                .stream()
                .map(this::resumir)
                .toList();
    }

    @Tool(name = "consultarPedido", description = "Consulta el detalle de un pedido perteneciente a un cliente.")
    @Transactional(readOnly = true)
    public PedidoResumen consultarPedido(
            @ToolParam(description = "Identificador del pedido") Long pedidoId,
            @ToolParam(description = "Identificador del cliente propietario del pedido") Long clienteId) {
        if (pedidoId == null || clienteId == null) {
            throw new IllegalArgumentException("Los identificadores del pedido y del cliente son obligatorios.");
        }
        Pedido pedido = pedidoRepository.findByIdAndClienteId(pedidoId, clienteId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró ese pedido para el cliente indicado."));
        return resumir(pedido);
    }

    private PedidoResumen resumir(Pedido pedido) {
        List<LineaPedidoResumen> lineas = pedido.getDetalles().stream()
                .map(detalle -> new LineaPedidoResumen(
                        detalle.getProducto().getId(),
                        detalle.getProducto().getNombre(),
                        detalle.getCantidad(),
                        detalle.getPrecioUnitario(),
                        detalle.getSubtotal()))
                .toList();
        return resumir(pedido, lineas);
    }

    private PedidoResumen resumir(Pedido pedido, List<LineaPedidoResumen> lineas) {
        return new PedidoResumen(
                pedido.getId(),
                pedido.getCliente().getId(),
                pedido.getDireccionEntrega(),
                pedido.getFechaCreacion(),
                pedido.getEstado(),
                lineas,
                pedido.getTotal());
    }

    public record LineaPedidoResumen(
            Long productoId,
            String nombreProducto,
            int cantidad,
            int precioUnitario,
            long subtotal) {
    }

    public record PedidoResumen(
            Long pedidoId,
            Long clienteId,
            String direccionEntrega,
            Instant fechaCreacion,
            EstadoPedido estado,
            List<LineaPedidoResumen> productos,
            long total) {
    }
}
