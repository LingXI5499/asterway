package com.asterway.common.result;

import java.util.List;

/** 分页返回对象，页码从 1 开始；items 与 total 使用相同的数据可见范围。 */
public record PageResultVO<T>(List<T> items, long total, int page, int size) {
}
