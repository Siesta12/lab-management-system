package com.nlt.scheduler;

import com.nlt.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 信用分自动恢复任务。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CreditRecoveryScheduler {

    private final UserService userService;

    /**
     * 每天凌晨自动恢复一次信用分，单次恢复 +1，最高不超过 100。
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void recoverCreditScores() {
        int updatedCount = userService.recoverCreditScores();
        log.info("Daily credit recovery finished, updated {} users.", updatedCount);
    }
}
