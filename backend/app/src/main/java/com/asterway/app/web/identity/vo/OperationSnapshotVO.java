package com.asterway.app.web.identity.vo;

import com.asterway.identity.api.model.OperationSnapshot;

import java.util.UUID;

public record OperationSnapshotVO(
        String operation,
        UUID idempotencyKey,
        String state,
        int httpStatus,
        String resultCode,
        Object result
) {

    public static OperationSnapshotVO from(OperationSnapshot value) {
        OperationSnapshotVO converted = new OperationSnapshotVO(
                value.operation(),
                value.idempotencyKey(),
                value.state(),
                value.httpStatus(),
                value.resultCode(),
                value.result()
        );
        return converted;
    }
}
