package com.erp.store;

import com.erp.domain.ErpModels.BugDefinition;
import com.erp.domain.ErpModels.BugReport;
import com.erp.domain.ErpModels.CompetitionFileSubmission;
import com.erp.domain.ErpModels.DocumentRecord;
import com.erp.domain.ErpModels.MasterRecord;
import com.erp.domain.ErpModels.Message;
import com.erp.domain.ErpModels.RankingHistory;
import com.erp.domain.ErpModels.SettlementRecord;
import com.erp.domain.ErpModels.StockBalance;
import com.erp.domain.ErpModels.StudentOperationLog;
import com.erp.domain.ErpModels.User;

import java.util.Collection;
import java.util.Map;

public interface ErpRealtimeRepository {
    boolean hasBusinessData();

    ErpStoreData loadBusinessData();

    void insertInitialData(ErpStoreData data);

    void updateUser(User user);

    void updateUserPasswordHashes(Map<Long, String> passwordHashes);

    void insertStudentWorkspace(User student, Collection<User> workspaceUsers, Collection<MasterRecord> workspaceMasters);

    void deleteStudentWorkspace(Long studentId, Collection<Long> removedUserIds);

    void updateBugDefinition(BugDefinition bug);

    void insertBugReport(BugReport report, Long workspaceId);

    void updateBugReport(BugReport report);

    void insertCompetitionFile(CompetitionFileSubmission file, Long workspaceId);

    void updateCompetitionFile(CompetitionFileSubmission file);

    void insertRankingHistory(RankingHistory history, Long workspaceId);

    void insertOperationLog(StudentOperationLog log, Long workspaceId);

    void upsertMaster(MasterRecord record);

    default void upsertMasters(Collection<MasterRecord> records) {
        records.forEach(this::upsertMaster);
    }

    void deleteMaster(MasterRecord record);

    void insertDocument(DocumentRecord document);

    void updateDocument(DocumentRecord document);

    void deleteDocument(DocumentRecord document);

    void upsertStock(StockBalance stock);

    void insertSettlement(SettlementRecord settlement, DocumentRecord document);

    void insertMessage(Message message);
}
