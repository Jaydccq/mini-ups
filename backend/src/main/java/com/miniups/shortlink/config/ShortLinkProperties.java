package com.miniups.shortlink.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "shortlink")
public class ShortLinkProperties {

    private Code code = new Code();
    private Bloom bloom = new Bloom();
    private Stream stream = new Stream();
    private Sentinel sentinel = new Sentinel();
    private Sharding sharding = new Sharding();

    public Code getCode() {
        return code;
    }

    public void setCode(Code code) {
        this.code = code;
    }

    public Bloom getBloom() {
        return bloom;
    }

    public void setBloom(Bloom bloom) {
        this.bloom = bloom;
    }

    public Stream getStream() {
        return stream;
    }

    public void setStream(Stream stream) {
        this.stream = stream;
    }

    public Sentinel getSentinel() {
        return sentinel;
    }

    public void setSentinel(Sentinel sentinel) {
        this.sentinel = sentinel;
    }

    public Sharding getSharding() {
        return sharding;
    }

    public void setSharding(Sharding sharding) {
        this.sharding = sharding;
    }

    public static class Code {
        /** Minimum length of generated short code. */
        private int minLength = 6;

        public int getMinLength() {
            return minLength;
        }

        public void setMinLength(int minLength) {
            this.minLength = minLength;
        }
    }

    public static class Bloom {
        private String redisKey = "shortlink:bloom:codes";
        private int bitSize = 1_048_576; // 1 MiB bitmap
        private java.util.List<Integer> hashSeeds = java.util.Arrays.asList(17, 31, 73, 127, 191);

        public String getRedisKey() {
            return redisKey;
        }

        public void setRedisKey(String redisKey) {
            this.redisKey = redisKey;
        }

        public int getBitSize() {
            return bitSize;
        }

        public void setBitSize(int bitSize) {
            this.bitSize = bitSize;
        }

        public java.util.List<Integer> getHashSeeds() {
            return hashSeeds;
        }

        public void setHashSeeds(java.util.List<Integer> hashSeeds) {
            this.hashSeeds = hashSeeds;
        }
    }

    public static class Stream {
        private String key = "stream:shortlink:monitor";
        private String group = "shortlink-monitor";
        private String consumerName = "backend-consumer";
        private int batchSize = 100;
        private Duration idleTimeout = Duration.ofMinutes(1);
        private long pollDelayMillis = 5000;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getGroup() {
            return group;
        }

        public void setGroup(String group) {
            this.group = group;
        }

        public String getConsumerName() {
            return consumerName;
        }

        public void setConsumerName(String consumerName) {
            this.consumerName = consumerName;
        }

        public int getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }

        public Duration getIdleTimeout() {
            return idleTimeout;
        }

        public void setIdleTimeout(Duration idleTimeout) {
            this.idleTimeout = idleTimeout;
        }

        public long getPollDelayMillis() {
            return pollDelayMillis;
        }

        public void setPollDelayMillis(long pollDelayMillis) {
            this.pollDelayMillis = pollDelayMillis;
        }
    }

    public static class Sentinel {
        private int createThresholdPerSecond = 20;
        private int redirectThresholdPerSecond = 200;

        public int getCreateThresholdPerSecond() {
            return createThresholdPerSecond;
        }

        public void setCreateThresholdPerSecond(int createThresholdPerSecond) {
            this.createThresholdPerSecond = createThresholdPerSecond;
        }

        public int getRedirectThresholdPerSecond() {
            return redirectThresholdPerSecond;
        }

        public void setRedirectThresholdPerSecond(int redirectThresholdPerSecond) {
            this.redirectThresholdPerSecond = redirectThresholdPerSecond;
        }
    }

    public static class Sharding {
        /** Weighted table definition, e.g. short_links_0:4,short_links_1:3 */
        private String tableWeights = "short_links_0:4,short_links_1:3,short_links_2:2,short_links_3:1";

        public String getTableWeights() {
            return tableWeights;
        }

        public void setTableWeights(String tableWeights) {
            this.tableWeights = tableWeights;
        }
    }
}
