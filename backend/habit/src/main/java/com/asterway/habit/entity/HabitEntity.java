package com.asterway.habit.entity;

import java.time.Instant;
import java.time.LocalDate;

/** 单表持久化对象；所有归属和写入时间由服务端维护。 */
public class HabitEntity {
    /** 习惯标识。 */
    private Long id;

    /** 所属用户。 */
    private Long userId;

    /** ACTIVE／PAUSED／ENDED。 */
    private String status;

    /** 当前完整修订号。 */
    private Long currentRevisionNo;

    /** 结束归档时间。 */
    private Instant endedAt;

    /** 已封存到的日期，只能前进。 */
    private LocalDate historyLockedThrough;

    /** 乐观锁版本。 */
    private Long version;

    /** 创建时间。 */
    private Instant createdAt;

    /** 更新时间。 */
    private Instant updatedAt;

    /** 逻辑删除时间。 */
    private Instant deletedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getCurrentRevisionNo() {
        return currentRevisionNo;
    }

    public void setCurrentRevisionNo(Long currentRevisionNo) {
        this.currentRevisionNo = currentRevisionNo;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(Instant endedAt) {
        this.endedAt = endedAt;
    }

    public LocalDate getHistoryLockedThrough() {
        return historyLockedThrough;
    }

    public void setHistoryLockedThrough(LocalDate historyLockedThrough) {
        this.historyLockedThrough = historyLockedThrough;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }
}
