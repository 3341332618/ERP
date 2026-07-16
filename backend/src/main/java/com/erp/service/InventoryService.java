package com.erp.service;

import com.erp.dto.InventoryDtos.RejectRequest;
import com.erp.store.ErpStore;
import org.springframework.stereotype.Service;

@Service
public class InventoryService {
    private final ErpStore store;

    public InventoryService(ErpStore store) {
        this.store = store;
    }

    public Object stock(String username) {
        return stock(username, null);
    }

    public Object stock(String username, Long editingId) {
        var user = store.userByUsername(username);
        return store.stockViews(user.id, editingId);
    }

    public Object auditList(String username, String direction) {
        var user = store.userByUsername(username);
        return store.auditList(direction, user.id);
    }

    public Object approve(String username, Long id) {
        var user = store.userByUsername(username);
        return store.approve(id, user.id);
    }

    public Object reject(String username, Long id, RejectRequest request) {
        var user = store.userByUsername(username);
        return store.reject(id, user.id, request.reason());
    }
}
