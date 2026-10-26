package com.asterway.app.application.identity;

import com.asterway.identity.api.AccountApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// 只处理账户已经登记的到期时区，不创建执行、计划或统计等其他业务。
@Component
public class AccountMaintenance {

    private static final Logger log = LoggerFactory.getLogger(AccountMaintenance.class);

    private final AccountApi accounts;

    public AccountMaintenance(AccountApi accounts) {
        this.accounts = accounts;
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
    public void applyPendingTimeZones() {
        try {
            accounts.applyPendingTimeZones();
        } catch (RuntimeException exception) {
            log.error("读取账户到期维护任务失败", exception);
        }
    }
}
