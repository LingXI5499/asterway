package com.asterway.app.web.identity.dto;

import com.asterway.common.error.CommonErrorCode;
import com.asterway.identity.api.error.AccountException;
import com.asterway.identity.api.model.RegistrationInput;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

// 注册只绑定允许提交的文本字段，省略值交由业务校验。
@JsonDeserialize(using = RegisterDTO.Deserializer.class)
public record RegisterDTO(
        String username,
        String nickname,
        String password,
        String confirmPassword,
        String zoneId,
        UUID captchaChallengeId,
        String captchaVerificationToken
) {

    public RegistrationInput toInput() {
        RegistrationInput converted = new RegistrationInput(
                username, nickname, password, confirmPassword, zoneId, captchaChallengeId, captchaVerificationToken
        );
        return converted;
    }

    @Override
    public String toString() {
        return "RegisterDTO[protected]";
    }

    public static final class Deserializer extends JsonDeserializer<RegisterDTO> {

        private static final Set<String> ALLOWED_FIELDS = Set.of(
                "username", "nickname", "password", "confirmPassword", "zoneId",
                "captchaChallengeId", "captchaVerificationToken"
        );

        @Override
        public RegisterDTO deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            JsonNode body = parser.getCodec().readTree(parser);
            if (!body.isObject()) {
                throw invalid();
            }
            body.fieldNames().forEachRemaining(field -> {
                if (!ALLOWED_FIELDS.contains(field) || !body.get(field).isTextual()) {
                    throw invalid();
                }
            });
            UUID challengeId = null;
            if (body.has("captchaChallengeId")) {
                String value = body.get("captchaChallengeId").asText();
                if (!value.matches("[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}")) {
                    throw invalid();
                }
                challengeId = UUID.fromString(value);
            }
            RegisterDTO converted = new RegisterDTO(
                    text(body, "username"), text(body, "nickname"), text(body, "password"),
                    text(body, "confirmPassword"), text(body, "zoneId"), challengeId,
                    text(body, "captchaVerificationToken")
            );
            return converted;
        }

        @Override
        public RegisterDTO getNullValue(DeserializationContext context) {
            throw invalid();
        }

        private String text(JsonNode body, String field) {
            return body.has(field) ? body.get(field).asText() : null;
        }

        private AccountException invalid() {
            AccountException converted = new AccountException(CommonErrorCode.VALIDATION_ERROR, Map.of());
            return converted;
        }
    }
}
