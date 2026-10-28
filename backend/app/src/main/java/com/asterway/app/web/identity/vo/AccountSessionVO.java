package com.asterway.app.web.identity.vo;

import com.asterway.identity.api.model.AccountSessionSnapshot;

import java.time.Instant;
import java.time.LocalDate;

public record AccountSessionVO(
        AccountVO account,
        CsrfVO csrf,
        Instant serverTime,
        LocalDate businessDate,
        LocalDate weekStartDate,
        LocalDate weekEndDate,
        boolean hasUnfinishedExecutions
) {

    public static AccountSessionVO from(AccountSessionSnapshot snapshot, CsrfVO csrf) {
        AccountSessionVO converted = new AccountSessionVO(
                AccountVO.from(snapshot.account()), csrf, snapshot.serverTime(), snapshot.businessDate(),
                snapshot.weekStartDate(), snapshot.weekEndDate(), snapshot.hasUnfinishedExecutions()
        );
        return converted;
    }
}
