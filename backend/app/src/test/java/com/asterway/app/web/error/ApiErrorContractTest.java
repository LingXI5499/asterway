package com.asterway.app.web.error;

import com.asterway.app.web.contract.ApiResponse;
import com.asterway.app.web.filter.TraceIdFilter;
import com.asterway.common.error.CommonErrorCode;
import com.asterway.common.error.ErrorCode;
import com.asterway.common.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApiErrorContractTest {

    @Test
    void shouldHideUnknownBusinessErrorDetails() {
        ErrorCode unknownCode = new ErrorCode() {
            @Override
            public String code() {
                return "UNKNOWN_INTERNAL_CODE";
            }

            @Override
            public String message() {
                return "sensitive internal detail";
            }
        };

        ResponseEntity<ApiResponse<Void>> response = new ApiErrorFactory()
                .fromBusinessException(new BusinessException(unknownCode), "test-trace-unknown");

        assertEquals(500, response.getStatusCode().value());
        ApiResponse<Void> body = response.getBody();
        assertNotNull(body);
        assertEquals("INTERNAL_ERROR", body.code());
        assertEquals("服务器内部错误", body.message());
        assertNull(body.data());
        assertEquals("test-trace-unknown", body.traceId());
    }

    // 测试一：验证错误工厂的状态码和响应数据
    @Test
    void shouldMapNotFoundExceptionCorrectly() {

        ApiErrorFactory factory = new ApiErrorFactory();

        BusinessException exception =
                new BusinessException(CommonErrorCode.NOT_FOUND);

        String traceId = "test-trace-001";

        ResponseEntity<ApiResponse<Void>> response =
                factory.fromBusinessException(exception, traceId);

        assertEquals(404, response.getStatusCode().value());

        ApiResponse<Void> body = response.getBody();

        assertNotNull(body);
        assertEquals("NOT_FOUND", body.code());
        assertEquals("未找到请求的资源", body.message());
        assertNull(body.data());
        assertEquals(traceId, body.traceId());
    }

    // 测试二：验证 MVC 能够统一处理业务异常
    @Test
    void shouldReturnUnifiedResponseForBusinessException()
            throws Exception {

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new TestController())
                .setControllerAdvice(
                        new GlobalExceptionHandler(
                                new ApiErrorFactory()
                        )
                )
                .addFilters(new TraceIdFilter())
                .build();

        MvcResult result = mockMvc.perform(
                        get("/test/not-found")
                )
                .andExpect(status().isNotFound())
                .andExpect(header().exists("X-Trace-Id"))
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message")
                        .value("未找到请求的资源"))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andReturn();

        String traceId =
                result.getResponse().getHeader("X-Trace-Id");

        JsonNode body = new ObjectMapper().readTree(
                result.getResponse().getContentAsString()
        );

        assertNotNull(traceId);
        assertEquals(traceId, body.get("traceId").asText());
    }

    // 测试三：验证未知异常不会泄露内部错误详情
    @Test
    void shouldHideUnexpectedExceptionDetails()
            throws Exception {

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new TestController())
                .setControllerAdvice(
                        new GlobalExceptionHandler(
                                new ApiErrorFactory()
                        )
                )
                .addFilters(new TraceIdFilter())
                .build();

        MvcResult result = mockMvc.perform(
                        get("/test/internal-error")
                )
                .andExpect(status().isInternalServerError())
                .andExpect(header().exists("X-Trace-Id"))
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.code")
                        .value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message")
                        .value("服务器内部错误"))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andReturn();

        String traceId =
                result.getResponse().getHeader("X-Trace-Id");

        JsonNode body = new ObjectMapper().readTree(
                result.getResponse().getContentAsString()
        );

        assertNotNull(traceId);
        assertEquals(traceId, body.get("traceId").asText());

        assertFalse(
                result.getResponse()
                        .getContentAsString()
                        .contains("sensitive internal detail")
        );
    }

    // 测试四：验证字段校验失败能够返回详细错误列表
    @Test
    void shouldReturnFieldViolationsForInvalidRequest()
            throws Exception {

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new TestController())
                .setControllerAdvice(
                        new GlobalExceptionHandler(
                                new ApiErrorFactory()
                        )
                )
                .addFilters(new TraceIdFilter())
                .build();

        MvcResult result = mockMvc.perform(
                        post("/test/validation")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "username": "",
                                            "nickname": ""
                                        }
                                        """)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(header().exists("X-Trace-Id"))
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.code")
                        .value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message")
                        .value("请检查填写内容"))
                .andExpect(jsonPath("$.data.violations").isArray())
                .andExpect(jsonPath("$.data.violations.length()")
                        .value(2))
                .andReturn();

        JsonNode root = new ObjectMapper().readTree(
                result.getResponse().getContentAsString()
        );

        String traceId =
                result.getResponse().getHeader("X-Trace-Id");

        assertNotNull(traceId);
        assertEquals(traceId, root.path("traceId").asText());

        JsonNode violations =
                root.path("data").path("violations");

        boolean usernameInvalid = false;
        boolean nicknameInvalid = false;

        for (JsonNode violation : violations) {

            String field = violation.path("field").asText();
            String message = violation.path("message").asText();

            if ("username".equals(field)) {
                usernameInvalid =
                        "用户名不能为空".equals(message);
            }

            if ("nickname".equals(field)) {
                nicknameInvalid =
                        "昵称不能为空".equals(message);
            }
        }

        assertTrue(usernameInvalid);
        assertTrue(nicknameInvalid);
    }

    // 测试专用 Controller，不属于正式业务代码
    @RestController
    static class TestController {

        @GetMapping("/test/not-found")
        public void notFound() {
            throw new BusinessException(
                    CommonErrorCode.NOT_FOUND
            );
        }

        @GetMapping("/test/internal-error")
        public void internalError() {
            throw new IllegalStateException(
                    "sensitive internal detail"
            );
        }

        @PostMapping("/test/validation")
        public void validate(
                @Valid @RequestBody ValidationRequest request
        ) {
            // 请求校验成功时才会进入此方法。
        }
    }

    // 测试专用请求 DTO
    record ValidationRequest(

            @NotBlank(message = "用户名不能为空")
            String username,

            @NotBlank(message = "昵称不能为空")
            String nickname

    ) {
    }
}
