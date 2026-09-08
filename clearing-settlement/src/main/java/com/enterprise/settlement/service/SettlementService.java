package com.enterprise.settlement.service;

import com.enterprise.settlement.model.MerchantPosition;
import com.enterprise.settlement.model.SettledTransaction;
import com.enterprise.settlement.model.SettlementBatch;
import com.enterprise.settlement.repository.SettledTransactionRepository;
import com.enterprise.settlement.repository.SettlementBatchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Nets all unsettled transactions by merchant into a single settlement
 * batch. A real end-of-day settlement job is significantly more involved —
 * it reconciles against the card network's own settlement files, handles
 * disputes/chargebacks that arrived after authorization, and produces the
 * actual fund transfer instructions to move money between issuer and
 * acquirer bank accounts (typically via ACH/wire, or a network-specific
 * settlement mechanism). This demonstrates the netting/batching pattern:
 * grouping many individual authorizations into net positions, which is the
 * core idea end-of-day settlement is built on.
 */
@Service
public class SettlementService {

    private static final Logger log = LoggerFactory.getLogger(SettlementService.class);

    private final SettledTransactionRepository transactionRepository;
    private final SettlementBatchRepository batchRepository;

    public SettlementService(SettledTransactionRepository transactionRepository,
                              SettlementBatchRepository batchRepository) {
        this.transactionRepository = transactionRepository;
        this.batchRepository = batchRepository;
    }

    @Transactional
    public SettlementBatch runSettlement() {
        List<SettledTransaction> unsettled = transactionRepository.findBySettlementBatchIdIsNull();

        if (unsettled.isEmpty()) {
            log.info("No unsettled transactions found; skipping batch creation");
            return null;
        }

        BigDecimal total = unsettled.stream()
                .map(SettledTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        SettlementBatch batch = batchRepository.save(new SettlementBatch(total, unsettled.size()));

        for (SettledTransaction txn : unsettled) {
            txn.setSettlementBatchId(batch.getId());
        }
        transactionRepository.saveAll(unsettled);

        log.info("Settlement batch {} created: {} transactions, total {}",
                batch.getId(), unsettled.size(), total);

        return batch;
    }

    public List<MerchantPosition> netPositionsForBatch(String batchId) {
        List<SettledTransaction> transactions = transactionRepository.findAll().stream()
                .filter(t -> batchId.equals(t.getSettlementBatchId()))
                .toList();

        Map<String, List<SettledTransaction>> byMerchant = transactions.stream()
                .collect(Collectors.groupingBy(SettledTransaction::getMerchantId));

        return byMerchant.entrySet().stream()
                .map(entry -> new MerchantPosition(
                        entry.getKey(),
                        entry.getValue().stream().map(SettledTransaction::getAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add),
                        entry.getValue().size()))
                .toList();
    }
}
