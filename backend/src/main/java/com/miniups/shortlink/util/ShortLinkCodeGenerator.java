package com.miniups.shortlink.util;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.miniups.shortlink.config.ShortLinkProperties;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

@Component
public class ShortLinkCodeGenerator {

    private static final HashFunction HASH_FUNCTION = Hashing.murmur3_32_fixed();
    private final SecureRandom secureRandom = new SecureRandom();
    private final ShortLinkProperties properties;

    public ShortLinkCodeGenerator(ShortLinkProperties properties) {
        this.properties = properties;
    }

    public String generate(String originalUrl, long userId, int attempt) {
        String payload = userId + "|" + originalUrl + "|" + attempt + "|" + System.nanoTime() + "|" + secureRandom.nextInt();
        int hash = HASH_FUNCTION.hashString(payload, StandardCharsets.UTF_8).asInt();
        long positive = Integer.toUnsignedLong(hash);
        String code = Base62Encoder.encode(positive);
        int minLength = Math.max(properties.getCode().getMinLength(), 6);
        if (code.length() < minLength) {
            // pad with additional random characters to reach minimum length
            StringBuilder sb = new StringBuilder(code);
            while (sb.length() < minLength) {
                sb.append(Base62Encoder.encode(Integer.toUnsignedLong(secureRandom.nextInt(62))));
            }
            code = sb.substring(0, minLength);
        }
        return code;
    }
}

