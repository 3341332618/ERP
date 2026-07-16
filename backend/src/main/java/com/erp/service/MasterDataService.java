package com.erp.service;

import com.erp.domain.ErpModels.RoleCode;
import com.erp.domain.ErpModels.Status;
import com.erp.dto.MasterDataDtos.StatusRequest;
import com.erp.store.ErpStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class MasterDataService {
    private final ErpStore store;

    public MasterDataService(ErpStore store) {
        this.store = store;
    }

    public Object list(String username, String type, String keyword, String status) {
        var user = store.userByUsername(username);
        return store.masters(type, keyword, status, user.id);
    }

    public Object create(String username, String type, Map<String, String> payload) {
        var user = store.userByUsername(username);
        return store.createMaster(type, payload, user.id);
    }

    public Object importProducts(String username, List<Map<String, String>> rows) {
        var user = store.userByUsername(username);
        return store.importProducts(rows, user.id);
    }

    public Object update(String username, String type, Long id, Map<String, String> payload) {
        var user = store.userByUsername(username);
        return store.updateMaster(type, id, payload, user.id);
    }

    public Object changeStatus(String username, String type, Long id, StatusRequest request) {
        var user = store.userByUsername(username);
        return store.changeMasterStatus(type, id, Status.valueOf(request.status()), user.id);
    }

    public Object warehouseStaff(String username) {
        var user = store.userByUsername(username);
        return store.usersByRole(RoleCode.WAREHOUSE_STAFF, user.id);
    }
}
