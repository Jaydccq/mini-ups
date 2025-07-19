/**
 * HTTP Client Configuration with Connection Pooling
 *
 * Functionality:
 * - Configure pooled HTTP client for high-performance inter-system communication
 * - Provide HTTP request support for Amazon integration
 * - Set up connection pool, timeout, retry parameters
 *
 * Configuration Features:
 * - RestTemplate with Apache HttpClient5 connection pooling
 * - Connection and read timeout settings
 * - Connection pool management for high-concurrency scenarios
 * - Configurable pool sizes and timeouts
 *
 * Performance Benefits:
 * - Connection reuse reduces TCP handshake overhead
 * - Pool management prevents connection exhaustion
 * - Optimized for high-throughput scenarios
 *
 * @author Mini-UPS Team
 * @version 2.0.0
 */
package com.miniups.config;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class HttpClientConfig {

    @Value("${http.client.max-total-connections:100}")
    private int maxTotalConnections;

    @Value("${http.client.max-default-per-route:20}")
    private int maxDefaultPerRoute;

    @Value("${http.client.connect-timeout-ms:10000}")
    private int connectTimeout;

    @Value("${http.client.socket-timeout-ms:30000}")
    private int socketTimeout;

    /**
     * Configure RestTemplate with connection pooling for high-performance HTTP communication
     *
     * @return Configured RestTemplate instance with pooled connections
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate(clientHttpRequestFactory());
    }

    /**
     * Configure HTTP request factory with Apache HttpClient5 connection pooling
     * Optimized for high-concurrency scenarios to prevent connection exhaustion
     *
     * @return HTTP request factory with connection pool
     */
    @Bean
    public ClientHttpRequestFactory clientHttpRequestFactory() {
        // Configure connection pool manager
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(maxTotalConnections);
        connectionManager.setDefaultMaxPerRoute(maxDefaultPerRoute);

        // Build HTTP client with connection pool
        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .build();

        // Configure request factory with timeouts
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
        factory.setConnectTimeout(connectTimeout);
        factory.setConnectionRequestTimeout(5000); // Pool acquisition timeout
        
        return factory;
    }
}