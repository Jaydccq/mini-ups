import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.atomic.AtomicLong;

/**
 * HTTP QPS/Latency Bench for /api/observability/qps/test
 *
 * Usage:
 *   javac scripts/benchmarks/HttpQpsBench.java && 
 *   java scripts.benchmarks.HttpQpsBench 100 30
 *     => 100 threads for 30 seconds
 */
public class HttpQpsBench {
    private static final String URL = "http://localhost:8081/api/observability/qps/test";

    public static void main(String[] args) throws Exception {
        int threads = args.length > 0 ? Integer.parseInt(args[0]) : 100;
        int seconds = args.length > 1 ? Integer.parseInt(args[1]) : 30;

        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        LongAdder total = new LongAdder();
        LongAdder success = new LongAdder();
        LongAdder fail = new LongAdder();
        AtomicLong totalLatencyNs = new AtomicLong();
        AtomicLong maxLatencyNs = new AtomicLong();

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        final boolean[] running = {true};

        Runnable worker = () -> {
            while (running[0]) {
                long start = System.nanoTime();
                try {
                    HttpRequest req = HttpRequest.newBuilder()
                            .uri(URI.create(URL))
                            .timeout(Duration.ofSeconds(5))
                            .header("X-Correlation-ID", "bench-" + System.nanoTime())
                            .GET().build();
                    HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
                    long end = System.nanoTime();
                    long dur = end - start;
                    total.increment();
                    if (resp.statusCode() == 200) {
                        success.increment();
                        totalLatencyNs.addAndGet(dur);
                        long prev;
                        while (dur > (prev = maxLatencyNs.get()) && !maxLatencyNs.compareAndSet(prev, dur)) {}
                    } else {
                        fail.increment();
                    }
                } catch (Exception e) {
                    total.increment();
                    fail.increment();
                }
            }
        };

        for (int i = 0; i < threads; i++) pool.submit(worker);
        Thread.sleep(seconds * 1000L);
        running[0] = false;
        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.SECONDS);

        long totalReq = total.sum();
        long ok = success.sum();
        long ko = fail.sum();
        double avgMs = ok > 0 ? (totalLatencyNs.get() / 1_000_000.0) / ok : 0.0;

        System.out.println("=== HTTP QPS Bench Results ===");
        System.out.printf("Threads: %d, Duration: %ds\n", threads, seconds);
        System.out.printf("Total: %,d, Success: %,d, Fail: %,d\n", totalReq, ok, ko);
        System.out.printf("Achieved QPS: %,.0f\n", totalReq / (double) seconds);
        System.out.printf("Average Latency: %.2f ms, Max: %.2f ms\n", avgMs, maxLatencyNs.get() / 1_000_000.0);
        System.out.println("Target (<5ms @100 threads): " + (threads == 100 && avgMs < 5.0 ? "PASSED" : "N/A / FAILED"));
    }
}

