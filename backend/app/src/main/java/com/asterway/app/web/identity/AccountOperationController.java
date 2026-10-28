package com.asterway.app.web.identity;

import com.asterway.app.security.AccountContextResolver;
import com.asterway.app.service.AccountIdempotencyService;
import com.asterway.app.web.contract.ApiResponse;
import com.asterway.app.web.filter.TraceIdFilter;
import com.asterway.app.web.identity.dto.OperationQueryDTO;
import com.asterway.app.web.identity.vo.OperationSnapshotVO;
import com.asterway.identity.api.model.AccountContext;
import com.asterway.identity.api.model.OperationSnapshot;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

// 查询本人已记录的操作结果，不重放命令或重新建立会话。
@RestController
@Tag(name = "账户辅助接口")
public class AccountOperationController {

    private final AccountIdempotencyService operations;
    private final AccountContextResolver contexts;

    public AccountOperationController(AccountIdempotencyService operations, AccountContextResolver contexts) {
        this.operations = operations;
        this.contexts = contexts;
    }

    @Operation(summary = "核实本人账户操作结果", description = "查询不到不代表未执行，不重发会话或执行原操作。")
    @GetMapping("/api/v1/operations/{idempotencyKey}")
    public ApiResponse<OperationSnapshotVO> operation(
            @PathVariable UUID idempotencyKey,
            @ParameterObject @ModelAttribute OperationQueryDTO query,
            HttpServletRequest request
    ) throws MissingServletRequestParameterException {
        if (query.operation() == null) {
            throw new MissingServletRequestParameterException("operation", "String");
        }
        AccountContext context = contexts.context(request, idempotencyKey, false);
        OperationSnapshot result = operations.query(context, query.operation());
        return ApiResponse.success(
                OperationSnapshotVO.from(result), (String) request.getAttribute(TraceIdFilter.TRACE_ID_ATTRIBUTE)
        );
    }
}
