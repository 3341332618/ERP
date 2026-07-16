package com.erp.service;

import com.erp.store.ErpStore;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class DocumentService {
    private final ErpStore store;

    public DocumentService(ErpStore store) {
        this.store = store;
    }

    public Object list(String username, String type) {
        var user = store.userByUsername(username);
        return store.documents(type, user.id);
    }

    public Object returnOptions(String username, String type, Long editingId) {
        var user = store.userByUsername(username);
        return store.returnOptions(user.id, type, editingId);
    }

    public Object create(String username, String type, Map<String, Object> payload) {
        var user = store.userByUsername(username);
        return store.createDocument(type, user.id, ServicePayloads.objectMap(payload));
    }

    public Object update(String username, String type, Long id, Map<String, Object> payload) {
        var user = store.userByUsername(username);
        return store.updateDocument(type, id, user.id, ServicePayloads.objectMap(payload));
    }

    public Object detail(String username, String type, Long id) {
        var user = store.userByUsername(username);
        return store.documentDetail(type, id, user.id);
    }

    public Object submit(String username, Long id) {
        var user = store.userByUsername(username);
        return store.submitDocument(id, user.id);
    }

    public Object delete(String username, Long id) {
        var user = store.userByUsername(username);
        return store.deleteDocument(id, user.id);
    }
}
