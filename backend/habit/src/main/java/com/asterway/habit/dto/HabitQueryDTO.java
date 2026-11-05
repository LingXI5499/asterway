package com.asterway.habit.dto;

import com.asterway.common.dto.PageQueryDTO;

/** 本人列表筛选，不接收所属用户标识。 */
public class HabitQueryDTO extends PageQueryDTO {
    private String keyword;
    private String status;
    private Long categoryId;
    private Boolean archived;

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Boolean getArchived() {
        return archived;
    }

    public void setArchived(Boolean archived) {
        this.archived = archived;
    }

    public int getOffset() {
        return offset();
    }
}
