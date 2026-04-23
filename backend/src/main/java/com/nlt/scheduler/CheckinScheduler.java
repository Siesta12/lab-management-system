package com.nlt.scheduler;

import com.nlt.service.CheckinService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 签到相关定时任务。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CheckinScheduler {

    private final CheckinService checkinService;

    /**
     * 每 5 分钟扫描一次超出签到窗口但仍未签到的预约，并记为爽约。
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void checkNoShowReservations() {
        int handledCount = checkinService.handleNoShowReservations();
    }

    /**
     * 每 5 分钟扫描一次已到下课时间但仍处于已通过状态的预约，自动改为已完成。
     */
    @Scheduled(cron = "30 */5 * * * ?")
    public void completeFinishedReservations() {
        int handledCount = checkinService.handleAutoCompleteReservations();
    }
}
