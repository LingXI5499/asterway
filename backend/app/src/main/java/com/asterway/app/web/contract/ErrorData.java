package com.asterway.app.web.contract;

import java.util.List;

// 参数校验失败时返回的附加数据。
// 里面是一组字段校验错误（Violation）。
public record ErrorData(
        List<Violation> violations  // 各字段的校验错误列表
) {
}
