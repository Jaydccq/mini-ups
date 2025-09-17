import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DB Write Contention Benchmark (before/after write-behind)
 *
 * Prerequisites:
 * - Backend running locally at http://localhost:8081
 * - Java 11+
 *
 * Usage:
 *   javac scripts/benchmarks/DbWriteContentionBench.java && \
 *   java scripts.benchmarks.DbWriteContentionBench 5000
 */
public class DbWriteContentionBench {
    private static final String BASE = "http://localhost:8081";
    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public static void main(String[] args) throws Exception {
        int ops = args.length > 0 ? Integer.parseInt(args[0]) : 5000;
        System.out.println("=== DB Write Contention Benchmark ===");
        System.out.println("Operations per run: " + ops);

        // Clean slate
        post("/api/debug/observability/cache/reset-metrics");

        // Baseline: write-behind OFF
        post("/api/debug/observability/cache/toggle?enabled=false");
        long t0 = System.currentTimeMillis();
        HttpResponse<String> baseResp = post("/api/debug/observability/cache/stress-test-open?operations=" + ops);
        long baseMs = System.currentTimeMillis() - t0;
        long baseDbWrites = extractLong(baseResp.body(), "\"databaseWrites\":(\d+)");
        System.out.printf("Baseline OFF -> duration=%dms, dbWrites=%d\n", baseMs, baseDbWrites);

        // Treatment: write-behind ON
        post("/api/debug/observability/cache/reset-metrics");
        post("/api/debug/observability/cache/toggle?enabled=true");
        long t1 = System.currentTimeMillis();
        HttpResponse<String> wbResp = post("/api/debug/observability/cache/stress-test-open?operations=" + ops);
        long wbMs = System.currentTimeMillis() - t1;
        long wbDbWrites = extractLong(wbResp.body(), "\"databaseWrites\":(\d+)");
        System.out.printf("Write-behind ON -> duration=%dms, dbWrites=%d\n", wbMs, wbDbWrites);

        // Compute reductions
        double contentionReduction = baseDbWrites > 0 ? (1.0 - (double) wbDbWrites / baseDbWrites) * 100.0 : 0.0;
        double latencyReduction = baseMs > 0 ? (1.0 - (double) wbMs / baseMs) * 100.0 : 0.0;

        System.out.println();
        System.out.printf("DB write contention reduction: %.1f%%\n", contentionReduction);
        System.out.printf("End-to-end latency reduction: %.1f%%\n", latencyReduction);
        System.out.println("======================================");
    }

    private static HttpResponse<String> post(String path) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + path))
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        return client.send(req, HttpResponse.BodyHandlers.ofString());
    }

    private static long extractLong(String body, String regex) {
        try {
            Pattern p = Pattern.compile(regex);
            Matcher m = p.matcher(body);
            if (m.find()) {
                return Long.parseLong(m.group(1));
            }
        } catch (Exception ignored) {}
        return -1;
    }
}

