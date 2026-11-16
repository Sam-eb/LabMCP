package com.example.labmcp.service;

import com.example.labmcp.model.Cliente;
import com.example.labmcp.repository.ClienteRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ClienteToolsService {
    private final ClienteRepository repository;

    public ClienteToolsService(ClienteRepository repository) {
        this.repository = repository;
    }

    @Tool(name = "registrarCliente", description = "Registra un cliente del supermercado para poder realizar pedidos.")
    @Transactional
    public Cliente registrarCliente(
            @ToolParam(description = "Nombre completo del cliente") String nombreCompleto,
            @ToolParam(description = "Número de cédula, debe ser único") String cedula,
            @ToolParam(description = "Dirección habitual del cliente") String direccion) {
        if (nombreCompleto == null || nombreCompleto.isBlank()
                || cedula == null || cedula.isBlank()
                || direccion == null || direccion.isBlank()) {
            throw new IllegalArgumentException("Nombre, cédula y dirección son obligatorios.");
        }
        if (repository.findByCedula(cedula.trim()).isPresent()) {
            throw new IllegalArgumentException("Ya existe un cliente registrado con esa cédula.");
        }
        return repository.save(new Cliente(nombreCompleto.trim(), cedula.trim(), direccion.trim()));
    }

    @Tool(name = "buscarClientePorCedula", description = "Busca un cliente registrado por su número de cédula.")
    @Transactional(readOnly = true)
    public Cliente buscarClientePorCedula(
            @ToolParam(description = "Número de cédula del cliente") String cedula) {
        if (cedula == null || cedula.isBlank()) {
            throw new IllegalArgumentException("La cédula es obligatoria.");
        }
        return repository.findByCedula(cedula.trim())
                .orElseThrow(() -> new IllegalArgumentException("No se encontró un cliente con esa cédula."));
    }
}
