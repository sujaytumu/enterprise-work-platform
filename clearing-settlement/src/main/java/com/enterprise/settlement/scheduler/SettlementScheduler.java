package com.enterprise.settlement.scheduler;

import com.enterprise.settlement.service.SettlementService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SettlementScheduler {

    private final SettlementService settlementService;

    public SettlementScheduler(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    /**
     * Runs at the configured cron time (default: 23:59 daily) to batch all
     * transactions authorized that day. Real end-of-day cutoffs are
     * typically timezone- and network-specific (e.g. aligned to a card
     * network's own processing calendar) — this uses a single simple cron
     * as a starting point.
     */
    @Scheduled(cron = "${settlement.cron:0 59 23 * * *}")
    public void runDailySettlement() {
        settlementService.runSettlement();
    }
}
