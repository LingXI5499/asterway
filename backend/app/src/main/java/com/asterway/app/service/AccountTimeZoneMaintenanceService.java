package com.asterway.app.service;

import com.asterway.identity.api.AccountTimeZoneApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

// 只处理账户已经登记的到期时区，不创建执行、计划或统计等其他业务。
@Service
public class AccountTimeZoneMaintenanceService {

    private static final Logger log = LoggerFactory.getLogger(AccountTimeZoneMaintenanceService.class);

    private final AccountTimeZoneApi accounts;

    public AccountTimeZoneMaintenanceService(AccountTimeZoneApi accounts) {
        this.accounts = accounts;
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
    public void applyPendingTimeZones() {
        try {
            accounts.applyPending();
        } catch (RuntimeException exception) {
            log.error("读取账户到期维护任务失败", exception);
        }
    }
}
