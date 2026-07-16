package com.erp.service;

import com.erp.store.ErpStore;
import org.springframework.stereotype.Service;

@Service
public class SettlementService {
    private final ErpStore store;

    public SettlementService(ErpStore store) {
        this.store = store;
    }

    public Object list(String username, String direction) {
        var user = store.userByUsername(username);
        return store.settlements(direction, user.id);
    }

    public Object detail(String username, String direction, Long id) {
        var user = store.userByUsername(username);
        return store.settlementDetail(direction, id, user.id);
    }
}
