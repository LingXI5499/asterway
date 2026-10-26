package com.asterway.identity.service;

import com.asterway.identity.api.model.AccountModels.PublicBootstrap;
import com.asterway.identity.api.model.AccountModels.TimeZoneItem;
import com.asterway.identity.api.model.AccountModels.TimeZonePage;
import com.asterway.identity.infrastructure.security.AccountCryptography;
import com.asterway.identity.infrastructure.serialization.AccountJsonCodec;
import com.asterway.identity.mapper.AccountLifecycleMapper;
import com.asterway.identity.validation.AccountValidator;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

// 公开数据只包含页面需要的固定配置与服务端支持的 IANA 名称。
@Service
public class AccountPublicQueryService {

    private final AccountLifecycleMapper mapper;
    private final AccountCryptography cryptography;
    private final AccountValidator validator;
    private final AccountJsonCodec jsonCodec;

    public AccountPublicQueryService(
            AccountLifecycleMapper mapper,
            AccountCryptography cryptography,
            AccountValidator validator,
            AccountJsonCodec jsonCodec
    ) {
        this.mapper = mapper;
        this.cryptography = cryptography;
        this.validator = validator;
        this.jsonCodec = jsonCodec;
    }

    public PublicBootstrap bootstrap() {
        PublicBootstrap bootstrap = new PublicBootstrap(mapper.siteName(), mapper.registrationOpen(), mapper.now().toInstant());
        return bootstrap;
    }

    public TimeZonePage timeZones(String query, int limit, String cursor) {
        if (limit < 1 || limit > 100) {
            validator.invalid("limit", "每页数量必须为1到100");
        }
        if (query != null) {
            validator.text(query, "q", 1, 64);
        }
        String filter = query == null ? "" : query.toLowerCase(Locale.ROOT);
        String after = cursor == null ? "" : readCursor(cursor, filter, limit);
        List<String> candidates = ZoneId.getAvailableZoneIds().stream()
                .filter(zone -> zone.toLowerCase(Locale.ROOT).contains(filter) && zone.compareTo(after) > 0)
                .sorted()
                .limit((long) limit + 1)
                .toList();
        boolean hasMore = candidates.size() > limit;
        List<TimeZoneItem> items = new ArrayList<>();
        for (int index = 0; index < Math.min(limit, candidates.size()); index++) {
            items.add(new TimeZoneItem(candidates.get(index)));
        }
        String next = null;
        if (hasMore) {
            Cursor nextCursor = new Cursor(filter, limit, items.get(items.size() - 1).zoneId());
            String body = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(jsonCodec.write(nextCursor).getBytes(StandardCharsets.UTF_8));
            next = body + "." + cryptography.digestHex("zone-cursor", body);
        }
        TimeZonePage page = new TimeZonePage(items, next, hasMore);
        return page;
    }

    private String readCursor(String value, String filter, int limit) {
        try {
            if (value.length() > 1024) {
                throw new IllegalArgumentException("cursor");
            }
            String[] parts = value.split("\\.", -1);
            if (parts.length != 2 || !MessageDigest.isEqual(
                    cryptography.digestHex("zone-cursor", parts[0]).getBytes(StandardCharsets.US_ASCII),
                    parts[1].getBytes(StandardCharsets.US_ASCII))) {
                throw new IllegalArgumentException("cursor");
            }
            String json = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            Cursor cursor = jsonCodec.read(json, Cursor.class);
            if (!filter.equals(cursor.filter()) || limit != cursor.limit()) {
                throw new IllegalArgumentException("cursor");
            }
            return cursor.after();
        } catch (IllegalArgumentException | IllegalStateException exception) {
            validator.invalid("cursor", "分页游标无效或与筛选条件不匹配");
            throw new IllegalStateException("游标校验未终止", exception);
        }
    }

    private record Cursor(String filter, int limit, String after) {
    }
}
