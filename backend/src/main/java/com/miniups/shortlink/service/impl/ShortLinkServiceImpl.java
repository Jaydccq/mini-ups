package com.miniups.shortlink.service.impl;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.EntryType;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.context.ContextUtil;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.miniups.shortlink.bloom.RedisBloomFilterService;
import com.miniups.shortlink.config.ShortLinkProperties;
import com.miniups.shortlink.dto.ShortLinkCreateRequest;
import com.miniups.shortlink.dto.ShortLinkPageResponse;
import com.miniups.shortlink.dto.ShortLinkResponse;
import com.miniups.shortlink.dto.ShortLinkUpdateRequest;
import com.miniups.shortlink.exception.ShortLinkConflictException;
import com.miniups.shortlink.exception.ShortLinkNotFoundException;
import com.miniups.shortlink.exception.ShortLinkRateLimitException;
import com.miniups.shortlink.exception.ShortLinkServiceException;
import com.miniups.shortlink.model.ShortLinkRecord;
import com.miniups.shortlink.model.ShortLinkRouteRecord;
import com.miniups.shortlink.repository.ShortLinkRepository;
import com.miniups.shortlink.repository.ShortLinkRouteRepository;
import com.miniups.shortlink.service.ShortLinkService;
import com.miniups.shortlink.sharding.ShortLinkShardUtils;
import com.miniups.shortlink.stream.ShortLinkStreamPublisher;
import com.miniups.shortlink.util.ShortLinkCodeGenerator;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Service
@Transactional
@ConditionalOnProperty(name = "shortlink.redis.enabled", havingValue = "true", matchIfMissing = true)
public class ShortLinkServiceImpl implements ShortLinkService {

    private static final Logger log = LoggerFactory.getLogger(ShortLinkServiceImpl.class);
    private static final String RESOURCE_CREATE = "shortlink-create";
    private static final String RESOURCE_REDIRECT = "shortlink-redirect";

    private final ShortLinkRepository shortLinkRepository;
    private final ShortLinkRouteRepository shortLinkRouteRepository;
    private final ShortLinkCodeGenerator codeGenerator;
    private final RedisBloomFilterService bloomFilterService;
    private final ShortLinkStreamPublisher streamPublisher;
    private final RedissonClient redissonClient;
    private final ShortLinkProperties properties;
    private final NavigableMap<Integer, String> tableWeightMap;
    private final MeterRegistry meterRegistry;
    private final java.util.concurrent.ConcurrentMap<String, Counter> shardCounters = new java.util.concurrent.ConcurrentHashMap<>();

    public ShortLinkServiceImpl(ShortLinkRepository shortLinkRepository,
                                ShortLinkRouteRepository shortLinkRouteRepository,
                                ShortLinkCodeGenerator codeGenerator,
                                RedisBloomFilterService bloomFilterService,
                                ShortLinkStreamPublisher streamPublisher,
                                RedissonClient redissonClient,
                                ShortLinkProperties properties,
                                MeterRegistry meterRegistry) {
        this.shortLinkRepository = shortLinkRepository;
        this.shortLinkRouteRepository = shortLinkRouteRepository;
        this.codeGenerator = codeGenerator;
        this.bloomFilterService = bloomFilterService;
        this.streamPublisher = streamPublisher;
        this.redissonClient = redissonClient;
        this.properties = properties;
        this.tableWeightMap = ShortLinkShardUtils.buildWeightMap(
                ShortLinkShardUtils.parseWeights(properties.getSharding().getTableWeights()));
        this.meterRegistry = meterRegistry;
    }

    @Override
    public ShortLinkResponse createShortLink(Long userId, ShortLinkCreateRequest request) {
        Long owner = userId == null ? -1L : userId;
        ContextUtil.enter(RESOURCE_CREATE, String.valueOf(owner));
        try (Entry ignored = SphU.entry(RESOURCE_CREATE, EntryType.OUT, 1, owner)) {
            String shortCode = resolveShortCode(owner, request);
            RReadWriteLock lock = redissonClient.getReadWriteLock(lockKey(shortCode));
            lock.writeLock().lock();
            try {
                ensureCodeAvailable(shortCode);
                LocalDateTime now = LocalDateTime.now();
                ShortLinkRecord record = new ShortLinkRecord();
                record.setShortCode(shortCode);
                record.setShardKey(shortCode);
                record.setOriginalUrl(request.getOriginalUrl());
                record.setUserId(owner);
                record.setExpirationAt(request.getExpirationAt());
                record.setActive(true);
                record.setAccessCount(0);
                record.setCreatedAt(now);
                record.setUpdatedAt(now);
                shortLinkRepository.insert(record);

                String tableName = ShortLinkShardUtils.resolveTable(shortCode, tableWeightMap);
                ShortLinkRouteRecord routeRecord = new ShortLinkRouteRecord();
                routeRecord.setShortCode(shortCode);
                routeRecord.setUserId(owner);
                routeRecord.setDataSource("ds0");
                routeRecord.setTableName(tableName);
                routeRecord.setOriginalUrl(request.getOriginalUrl());
                routeRecord.setCreatedAt(now);
                shortLinkRouteRepository.insertRoute(routeRecord);

                bloomFilterService.add(shortCode);
                incrementShardCounter(tableName);
                return toResponse(record, tableName);
            } finally {
                lock.writeLock().unlock();
            }
        } catch (BlockException ex) {
            throw new ShortLinkRateLimitException("Short link creation rate limit exceeded for user " + owner);
        } finally {
            ContextUtil.exit();
        }
    }

    @Override
    public ShortLinkResponse updateShortLink(Long userId, String shortCode, ShortLinkUpdateRequest request) {
        RReadWriteLock lock = redissonClient.getReadWriteLock(lockKey(shortCode));
        lock.writeLock().lock();
        try {
            ShortLinkRecord record = shortLinkRepository.findByShortCode(shortCode)
                    .orElseThrow(() -> new ShortLinkNotFoundException(shortCode));
            if (userId != null && record.getUserId() != null && !record.getUserId().equals(userId)) {
                throw new ShortLinkServiceException("You do not have permission to modify this short link");
            }
            String newUrl = request.getOriginalUrl() != null ? request.getOriginalUrl() : record.getOriginalUrl();
            LocalDateTime expiration = request.getExpirationAt() != null ? request.getExpirationAt() : record.getExpirationAt();
            boolean active = request.getActive() != null ? request.getActive() : record.isActive();
            LocalDateTime now = LocalDateTime.now();
            shortLinkRepository.updateOriginalUrl(shortCode, newUrl, now, expiration, active);
            shortLinkRouteRepository.updateOriginalUrl(shortCode, newUrl);
            record.setOriginalUrl(newUrl);
            record.setUpdatedAt(now);
            record.setExpirationAt(expiration);
            record.setActive(active);
            String tableName = ShortLinkShardUtils.resolveTable(shortCode, tableWeightMap);
            return toResponse(record, tableName);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ShortLinkResponse getShortLinkDetails(Long userId, String shortCode) {
        ShortLinkRecord record = shortLinkRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ShortLinkNotFoundException(shortCode));
        if (userId != null && record.getUserId() != null && !record.getUserId().equals(userId)) {
            throw new ShortLinkServiceException("You do not have permission to view this short link");
        }
        String tableName = ShortLinkShardUtils.resolveTable(shortCode, tableWeightMap);
        return toResponse(record, tableName);
    }

    @Override
    @Transactional(readOnly = true)
    public ShortLinkPageResponse listShortLinks(int page, int size) {
        List<ShortLinkRouteRecord> routes = shortLinkRouteRepository.listRoutes(page, size);
        List<ShortLinkResponse> responses = new ArrayList<>();
        for (ShortLinkRouteRecord route : routes) {
            Optional<ShortLinkRecord> recordOpt = shortLinkRepository.findByShortCode(route.getShortCode());
            recordOpt.ifPresent(record -> responses.add(toResponse(record, route.getTableName())));
        }
        ShortLinkPageResponse response = new ShortLinkPageResponse();
        response.setRecords(responses);
        response.setTotal(shortLinkRouteRepository.countRoutes());
        response.setPage(page);
        response.setSize(size);
        return response;
    }

    @Override
    public String resolveRedirect(String shortCode, HttpServletRequest request) {
        ShortLinkRecord record = shortLinkRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ShortLinkNotFoundException(shortCode));
        enforceExpiration(record);

        Long ownerId = record.getUserId() == null ? -1L : record.getUserId();
        ContextUtil.enter(RESOURCE_REDIRECT, String.valueOf(ownerId));
        try (Entry ignored = SphU.entry(RESOURCE_REDIRECT, EntryType.OUT, 1, ownerId)) {
            RReadWriteLock lock = redissonClient.getReadWriteLock(lockKey(shortCode));
            lock.readLock().lock();
            try {
                if (!record.isActive()) {
                    throw new ShortLinkServiceException("Short link is inactive");
                }
                LocalDateTime now = LocalDateTime.now();
                shortLinkRepository.incrementAccessCount(shortCode, now);

                String clientIp = extractClientIp(request);
                String userAgent = request.getHeader("User-Agent");

                streamPublisher.publishAccessEvent(record, clientIp, userAgent);

                return record.getOriginalUrl();
            } finally {
                lock.readLock().unlock();
            }
        } catch (BlockException ex) {
            throw new ShortLinkRateLimitException("Short link redirect rate limit exceeded for owner " + ownerId);
        } finally {
            ContextUtil.exit();
        }
    }

    private void ensureCodeAvailable(String shortCode) {
        if (bloomFilterService.mightContain(shortCode)) {
            shortLinkRepository.findByShortCode(shortCode).ifPresent(existing -> {
                throw new ShortLinkConflictException(shortCode);
            });
        }
    }

    private String resolveShortCode(Long userId, ShortLinkCreateRequest request) {
        if (StringUtils.hasText(request.getCustomCode())) {
            return request.getCustomCode().trim();
        }
        for (int attempt = 0; attempt < 5; attempt++) {
            String candidate = codeGenerator.generate(request.getOriginalUrl(), userId == null ? 0 : userId, attempt);
            if (!bloomFilterService.mightContain(candidate)
                    && shortLinkRepository.findByShortCode(candidate).isEmpty()) {
                return candidate;
            }
        }
        throw new ShortLinkServiceException("Unable to allocate unique short link code after multiple attempts");
    }

    private void enforceExpiration(ShortLinkRecord record) {
        if (record.getExpirationAt() != null && record.getExpirationAt().isBefore(LocalDateTime.now())) {
            shortLinkRepository.deactivateIfExpired(record.getShortCode(), LocalDateTime.now());
            record.setActive(false);
        }
    }

    private ShortLinkResponse toResponse(ShortLinkRecord record, String tableName) {
        ShortLinkResponse response = new ShortLinkResponse();
        response.setShortCode(record.getShortCode());
        response.setOriginalUrl(record.getOriginalUrl());
        response.setUserId(record.getUserId());
        response.setExpirationAt(record.getExpirationAt());
        response.setActive(record.isActive());
        response.setAccessCount(record.getAccessCount());
        response.setCreatedAt(record.getCreatedAt());
        response.setUpdatedAt(record.getUpdatedAt());
        response.setLastAccessAt(record.getLastAccessAt());
        response.setRouteTable(tableName);
        return response;
    }

    private String lockKey(String shortCode) {
        return "shortlink:lock:" + shortCode;
    }

    private String extractClientIp(HttpServletRequest request) {
        String header = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(header)) {
            return header.split(",")[0].trim();
        }
        header = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(header)) {
            return header.trim();
        }
        return request.getRemoteAddr();
    }

    private void incrementShardCounter(String tableName) {
        shardCounters.computeIfAbsent(tableName,
                t -> meterRegistry.counter("shortlink.shard.writes", "table", t)).increment();
    }
}
