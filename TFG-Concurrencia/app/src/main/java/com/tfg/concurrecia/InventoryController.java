package com.tfg.concurrencia;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping("/purchase/occ")
    public ResponseEntity<PurchaseResponse> purchaseOCC(@RequestBody PurchaseRequest request) {
        try {
            String result = inventoryService.purchaseOCC(request.getProductId(), request.getQuantity());
            return handleResult(result);
        } catch (Exception e) {
            // Captura timeouts de DB, pérdida de conexión, etc.
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new PurchaseResponse(false, "ERROR: " + e.getMessage()));
        }
    }

    @PostMapping("/purchase/pcc")
    public ResponseEntity<PurchaseResponse> purchasePCC(@RequestBody PurchaseRequest request) {
        try {
            String result = inventoryService.purchasePCC(request.getProductId(), request.getQuantity());
            return handleResult(result);
        } catch (Exception e) {
            // Captura Deadlocks (SQLState 40P01) y Lock Timeouts
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new PurchaseResponse(false, "ERROR: " + e.getMessage()));
        }
    }

    private ResponseEntity<PurchaseResponse> handleResult(String result) {
        return switch (result) {
            case "SUCCESS" -> ResponseEntity.ok(new PurchaseResponse(true, "SUCCESS"));
            case "CONFLICT" -> ResponseEntity.status(HttpStatus.CONFLICT).body(new PurchaseResponse(false, "CONFLICT"));
            case "OUT_OF_STOCK" -> ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new PurchaseResponse(false, "OUT_OF_STOCK"));
            default -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new PurchaseResponse(false, "UNKNOWN"));
        };
    }
}