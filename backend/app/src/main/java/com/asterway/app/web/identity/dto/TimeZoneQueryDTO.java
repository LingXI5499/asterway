package com.asterway.app.web.identity.dto;



public record TimeZoneQueryDTO(String q, Integer limit, String cursor) {

    public int effectiveLimit() {
        return limit == null ? 20 : limit;
    }
}
