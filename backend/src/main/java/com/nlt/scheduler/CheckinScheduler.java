package com.nlt.scheduler;

import com.nlt.service.CheckinService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CheckinScheduler {

    private final CheckinService checkinService;

    @Scheduled(cron = "0 */5 * * * ?")
    public void checkNoShowReservations() {
        int handledCount = checkinService.handleNoShowReservations();
    }

    @Scheduled(cron = "30 */5 * * * ?")
    public void completeFinishedReservations() {
        int handledCount = checkinService.handleAutoCompleteReservations();
    }
}
