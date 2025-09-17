package com.miniups.shortlink.config;

import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ShortLinkOpenApiConfig {

    @Bean
    public GroupedOpenApi shortLinkGroupedOpenApi() {
        return GroupedOpenApi.builder()
                .group("short-link")
                .addOpenApiCustomizer(openApi -> openApi.info(new Info()
                        .title("Short Link Service API")
                        .description("RBAC enabled short link APIs powered by MurmurHash, Redis Bloom, Sentinel and Redisson")
                        .version("v1")))
                .pathsToMatch("/api/short-links/**", "/short-links/**", "/s/**")
                .build();
    }
}
