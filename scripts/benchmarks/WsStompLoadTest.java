import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * STOMP over WebSocket load test using Java 11 standard WebSocket client.
 * Targets Spring's STOMP endpoint at ws://localhost:8081/ws.
 *
 * Usage:
 *   javac scripts/benchmarks/WsStompLoadTest.java && \
 *   java scripts.benchmarks.WsStompLoadTest 600 30
 *     => 600 concurrent connections, hold for 30s
 */
public class WsStompLoadTest {
    public static void main(String[] args) throws Exception {
        int connections = args.length > 0 ? Integer.parseInt(args[0]) : 500;
        int holdSeconds = args.length > 1 ? Integer.parseInt(args[1]) : 20;

        System.out.printf("=== WS STOMP Load Test: %d connections, hold %ds ===%n", connections, holdSeconds);

        HttpClient httpClient = HttpClient.newHttpClient();
        URI uri = URI.create("ws://localhost:8081/ws");

        AtomicInteger connected = new AtomicInteger();
        AtomicInteger failed = new AtomicInteger();
        List<WebSocket> sockets = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(connections);

        for (int i = 0; i < connections; i++) {
            WebSocket.Builder builder = httpClient.newWebSocketBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .subprotocols("v12.stomp");

            builder.buildAsync(uri, new WebSocket.Listener() {
                private WebSocket ws;

                @Override
                public void onOpen(WebSocket webSocket) {
                    this.ws = webSocket;
                    sockets.add(webSocket);
                    // Send STOMP CONNECT frame
                    String connect = "CONNECT\naccept-version:1.2\nhost:localhost\n\n\u0000";
                    webSocket.sendText(connect, true);
                    WebSocket.Listener.super.onOpen(webSocket);
                }

                @Override
                public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                    String msg = data.toString();
                    if (msg.startsWith("CONNECTED")) {
                        connected.incrementAndGet();
                        latch.countDown();
                    }
                    return WebSocket.Listener.super.onText(webSocket, data, last);
                }

                @Override
                public void onError(WebSocket webSocket, Throwable error) {
                    failed.incrementAndGet();
                    latch.countDown();
                }
            });
        }

        // Wait for all to connect (or timeout)
        latch.await(30, TimeUnit.SECONDS);
        System.out.printf("Connected: %d, Failed: %d%n", connected.get(), failed.get());

        // Hold connections
        Thread.sleep(holdSeconds * 1000L);

        // Cleanly disconnect
        String disconnect = "DISCONNECT\n\n\u0000";
        for (WebSocket ws : sockets) {
            try { ws.sendText(disconnect, true); } catch (Exception ignored) {}
            try { ws.sendClose(WebSocket.NORMAL_CLOSURE, "bye").join(); } catch (Exception ignored) {}
        }

        System.out.println("=== WS STOMP Load Test: DONE ===");
    }
}

