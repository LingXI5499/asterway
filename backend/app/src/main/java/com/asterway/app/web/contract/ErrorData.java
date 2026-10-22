package com.asterway.app.web.contract;

import java.util.List;

public record ErrorData(
        List<Violation> violations
) {
}
