package com.asterway.app.web.identity.dto;

import com.asterway.common.error.CommonErrorCode;
import com.asterway.identity.api.error.AccountException;
import com.asterway.identity.api.model.ProfileInput;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

// 省略用户名或昵称表示保留，显式 null、未知字段及非文本值仍拒绝。
@JsonDeserialize(using = ProfileUpdateDTO.Deserializer.class)
public record ProfileUpdateDTO(String expectedVersion, String username, String nickname) {

    public ProfileInput toInput() {
        ProfileInput converted = new ProfileInput(expectedVersion, username, nickname);
        return converted;
    }

    public static final class Deserializer extends JsonDeserializer<ProfileUpdateDTO> {

        private static final Set<String> ALLOWED_FIELDS = Set.of("username", "nickname", "expectedVersion");

        @Override
        public ProfileUpdateDTO deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            JsonNode body = parser.getCodec().readTree(parser);
            if (!body.isObject()) {
                throw invalid();
            }
            body.fieldNames().forEachRemaining(field -> {
                if (!ALLOWED_FIELDS.contains(field) || !body.get(field).isTextual()) {
                    throw invalid();
                }
            });
            ProfileUpdateDTO converted = new ProfileUpdateDTO(
                    text(body, "expectedVersion"), text(body, "username"), text(body, "nickname")
            );
            return converted;
        }

        @Override
        public ProfileUpdateDTO getNullValue(DeserializationContext context) {
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
