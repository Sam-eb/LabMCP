package com.example.labmcp.repository;

import com.example.labmcp.model.Pedido;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    @EntityGraph(attributePaths = {"detalles", "detalles.producto"})
    List<Pedido> findByClienteIdOrderByFechaCreacionDesc(Long clienteId);

    @EntityGraph(attributePaths = {"detalles", "detalles.producto"})
    Optional<Pedido> findByIdAndClienteId(Long pedidoId, Long clienteId);
}
