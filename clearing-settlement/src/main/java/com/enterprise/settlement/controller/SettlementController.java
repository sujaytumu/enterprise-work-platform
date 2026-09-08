package com.enterprise.settlement.controller;

import com.enterprise.settlement.model.MerchantPosition;
import com.enterprise.settlement.model.SettlementBatch;
import com.enterprise.settlement.service.SettlementService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/settlement")
public class SettlementController {

    private final SettlementService settlementService;

    public SettlementController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    /** Manual trigger for demo/testing — production relies on the scheduled cron job. */
    @PostMapping("/run")
    public ResponseEntity<SettlementBatch> runNow() {
        SettlementBatch batch = settlementService.runSettlement();
        return batch == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(batch);
    }

    @GetMapping("/{batchId}/positions")
    public ResponseEntity<List<MerchantPosition>> positions(@PathVariable String batchId) {
        return ResponseEntity.ok(settlementService.netPositionsForBatch(batchId));
    }
}
