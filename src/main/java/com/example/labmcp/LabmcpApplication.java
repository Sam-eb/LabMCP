package com.example.labmcp;

import com.example.labmcp.service.ClienteToolsService;
import com.example.labmcp.service.PedidoToolsService;
import com.example.labmcp.service.ProductoToolsService;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class LabmcpApplication {

	public static void main(String[] args) {
		SpringApplication.run(LabmcpApplication.class, args);
	}


	@Bean
	public ToolCallbackProvider supermercadoTools(
			ClienteToolsService clientes,
			ProductoToolsService productos,
			PedidoToolsService pedidos) {
		return MethodToolCallbackProvider.builder()
				.toolObjects(clientes, productos, pedidos)
				.build();
	}

}
