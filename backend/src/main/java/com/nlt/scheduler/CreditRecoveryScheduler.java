package com.nlt.scheduler;

import com.nlt.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CreditRecoveryScheduler {

    private final UserService userService;

    @Scheduled(cron = "0 0 2 * * ?")
    public void recoverCreditScores() {
        int updatedCount = userService.recoverCreditScores();
        log.info("Daily credit recovery finished, updated {} users.", updatedCount);
    }
}
