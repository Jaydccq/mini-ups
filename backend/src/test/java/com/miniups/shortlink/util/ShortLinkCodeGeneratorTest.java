package com.miniups.shortlink.util;

import com.miniups.shortlink.config.ShortLinkProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ShortLinkCodeGeneratorTest {

    private ShortLinkCodeGenerator codeGenerator;
    private ShortLinkProperties properties;

    @BeforeEach
    void setUp() {
        properties = new ShortLinkProperties();
        properties.getCode().setMinLength(8);
        codeGenerator = new ShortLinkCodeGenerator(properties);
    }

    @Test
    void generate_shouldProduceDeterministicLength() {
        String code = codeGenerator.generate("https://miniups.com/test", 42L, 0);
        assertThat(code).hasSizeGreaterThanOrEqualTo(properties.getCode().getMinLength());
    }

    @Test
    void generate_shouldProduceDifferentCodesOnAttempts() {
        String first = codeGenerator.generate("https://miniups.com/test", 42L, 0);
        String second = codeGenerator.generate("https://miniups.com/test", 42L, 1);
        assertThat(first).isNotEqualTo(second);
    }
}
