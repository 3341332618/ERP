package com.erp.dto;

public final class InventoryDtos {
    private InventoryDtos() {
    }

    public record RejectRequest(String reason) {
    }
}
