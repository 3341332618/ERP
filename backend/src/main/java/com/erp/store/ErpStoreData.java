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

import java.util.List;

public record ErpStoreData(
    long maxId,
    int warehouseSeq,
    int customerSeq,
    int supplierSeq,
    int productSeq,
    int documentSeq,
    int settlementSeq,
    List<User> users,
    List<MasterRecord> masters,
    List<DocumentRecord> documents,
    List<BugDefinition> bugDefinitions,
    List<Message> messages,
    List<StockBalance> stocks,
    List<SettlementRecord> settlements,
    List<BugReport> bugReports,
    List<CompetitionFileSubmission> competitionFiles,
    List<RankingHistory> rankingHistoryRecords,
    List<StudentOperationLog> studentOperationLogs
) {
}
