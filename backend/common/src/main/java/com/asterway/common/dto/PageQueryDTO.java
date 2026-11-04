package com.asterway.common.dto;

import com.asterway.common.exception.BusinessException;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** 通用分页请求；不接受前端拼接排序字段。 */
public class PageQueryDTO {

    @Min(1)
    private int page = 1;

    @Min(1)
    @Max(100)
    private int size = 20;

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    /** 在 Service 边界也限制分页，避免绕过 Controller 校验。 */
    public int offset() {
        if (page < 1 || size < 1 || size > 100 || (long) (page - 1) * size > Integer.MAX_VALUE) {
            throw BusinessException.badRequest("分页参数无效");
        }
        return (page - 1) * size;
    }
}
