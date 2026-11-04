package com.asterway.common.utils;

import com.asterway.common.exception.BusinessException;
import com.asterway.common.security.LoginSessionData;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Component;

/** JWT 签发与验签；固定算法和签发者，不把令牌中的权限作为授权依据。 */
@Component
public class JwtTokenUtils {

    private static final String ISSUER = "asterway";
    private final JwtEncoder encoder;
    private final JwtDecoder decoder;

    public JwtTokenUtils(@Value("${asterway.security.jwt-secret}") String secret) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("本地 JWT 签名配置至少需要32字节");
        }
        var key = new SecretKeySpec(bytes, "HmacSHA256");
        encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        NimbusJwtDecoder verifier = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        verifier.setJwtValidator(JwtValidators.createDefaultWithIssuer(ISSUER));
        decoder = verifier;
    }

    public String create(LoginSessionData session) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .audience(List.of("asterway-web"))
                .subject(session.userId())
                .issuedAt(Instant.ofEpochMilli(session.lastActiveAt()))
                .expiresAt(Instant.ofEpochMilli(session.expiresAt()))
                .claim("sid", session.id())
                .build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    public Jwt parse(String token) {
        Jwt jwt = decoder.decode(token);
        if (!jwt.getAudience().contains("asterway-web") || jwt.getClaimAsString("sid") == null
                || jwt.getExpiresAt() == null || !jwt.getExpiresAt().isAfter(Instant.now())) {
            throw BusinessException.unauthorized();
        }
        return jwt;
    }
}
