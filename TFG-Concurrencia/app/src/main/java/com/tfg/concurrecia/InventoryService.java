package com.tfg.concurrencia;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class InventoryService {

    private final JdbcTemplate jdbcTemplate;

    public InventoryService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ESTRATEGIA 1: Control de Concurrencia Optimista (OCC)
    public String purchaseOCC(Long productId, Integer quantity) {
        // 1. Leer estado actual
        String selectSql = "SELECT stock, version FROM inventario_producto WHERE product_id = ?";
        Map<String, Object> row = jdbcTemplate.queryForMap(selectSql, productId);
        
        Integer stock = (Integer) row.get("stock");
        Long version = (Long) row.get("version");

        if (stock < quantity) {
            return "OUT_OF_STOCK";
        }

        // 2. Intentar actualizar condicionando por la versión leída
        String updateSql = "UPDATE inventario_producto SET stock = stock - ?, version = version + 1 " +
                           "WHERE product_id = ? AND version = ?";
        
        int rowsAffected = jdbcTemplate.update(updateSql, quantity, productId, version);

        // Si rowsAffected es 0, alguien más modificó el registro entre nuestro SELECT y el UPDATE
        if (rowsAffected == 0) {
            return "CONFLICT";
        }

        return "SUCCESS";
    }

    // ESTRATEGIA 2: Control de Concurrencia Pesimista (PCC)
    // El @Transactional garantiza que el bloqueo se mantenga hasta terminar el UPDATE
    @Transactional
    public String purchasePCC(Long productId, Integer quantity) {
        // 1. Leer bloqueando la fila (SELECT ... FOR UPDATE)
        String selectSql = "SELECT stock FROM inventario_producto WHERE product_id = ? FOR UPDATE";
        Integer stock = jdbcTemplate.queryForObject(selectSql, Integer.class, productId);

        if (stock == null || stock < quantity) {
            return "OUT_OF_STOCK";
        }

        // 2. Actualizar (tenemos el bloqueo exclusivo, nadie más puede modificar ni bloquear esta fila)
        String updateSql = "UPDATE inventario_producto SET stock = stock - ? WHERE product_id = ?";
        jdbcTemplate.update(updateSql, quantity, productId);

        return "SUCCESS";
    }
}