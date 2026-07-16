package com.erp.service;

import com.erp.store.ErpStore;
import org.springframework.stereotype.Service;

@Service
public class SystemService {
    private final ErpStore store;

    public SystemService(ErpStore store) {
        this.store = store;
    }

    public Object messages(String username) {
        var user = store.userByUsername(username);
        return store.messages(user.id);
    }
}
