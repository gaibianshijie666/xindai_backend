package com.xindai.xindai.modules.loan.task;

import com.xindai.xindai.modules.loan.service.OverdueService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OverdueCheckTask {
    private final OverdueService overdueService;

    public OverdueCheckTask(OverdueService overdueService) {
        this.overdueService = overdueService;
    }

    @Scheduled(cron = "0 0 1 * * ?")
    public void checkOverdue() {
        log.info("开始执行逾期检查任务");
        try {
            overdueService.checkAndMarkOverdue();
            log.info("逾期检查任务执行完成");
        } catch (Exception e) {
            log.error("逾期检查任务执行失败", e);
        }
    }
}
