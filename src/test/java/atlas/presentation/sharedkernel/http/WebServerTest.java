package atlas.presentation.sharedkernel.http;

import static org.assertj.core.api.Assertions.assertThat;

import atlas.presentation.sharedkernel.sse.SseEvent;
import atlas.presentation.sharedkernel.sse.SseHub;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.Timeout.ThreadMode;

@Timeout(value = 15, threadMode = ThreadMode.SEPARATE_THREAD)
class WebServerTest {

    private final SseHub hub = new SseHub();
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    private WebServer server;

    @BeforeEach
    void startServer() throws IOException {
        var router = Router.builder()
            .mount(Routes.at("/appointments")
                .get("/{id}", r -> HttpResponse.ok("{\"id\":\"" + r.pathParam("id") + "\"}"))
                .post("/", r -> HttpResponse.noContent()))
            .build();

        server = WebServer.onLoopback(0).mount("/appointments", router).mount("/events", new SseEndpoint(hub)).start();
    }

    @AfterEach
    void stopServer() {
        hub.closeAll();
        server.close();
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + server.port() + path);
    }

    private java.net.http.HttpResponse<String> get(String path) throws Exception {
        var request = java.net.http.HttpRequest.newBuilder(uri(path)).GET().build();

        return client.send(request, BodyHandlers.ofString());
    }

    @Test
    void shouldServeARoutedRequest() throws Exception {
        var response = get("/appointments/A00000007");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("{\"id\":\"A00000007\"}");
        assertThat(response.headers().firstValue("Content-Type")).contains(HttpResponse.JSON);
    }

    @Test
    void shouldAnswerWithACorrelationIdHeader() throws Exception {
        var response = get("/appointments/A1");

        assertThat(response.headers().firstValue(Router.CORRELATION_HEADER)).isPresent();
    }

    @Test
    void shouldEchoTheCorrelationIdSuppliedByTheCaller() throws Exception {
        var request = java.net.http.HttpRequest
            .newBuilder(uri("/appointments/A1"))
            .header(Router.CORRELATION_HEADER, "trace42")
            .GET()
            .build();

        var response = client.send(request, BodyHandlers.ofString());

        assertThat(response.headers().firstValue(Router.CORRELATION_HEADER)).contains("trace42");
    }

    @Test
    void shouldSendNoBodyWhenTheResponseHasNoContent() throws Exception {
        var request = java.net.http.HttpRequest
            .newBuilder(uri("/appointments"))
            .POST(java.net.http.HttpRequest.BodyPublishers.ofString("{}"))
            .build();

        var response = client.send(request, BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(204);
        assertThat(response.body()).isEmpty();
    }

    @Test
    void shouldRejectABodyLargerThanTheLimit() throws Exception {
        var oversized = "x".repeat(Router.MAX_BODY_BYTES + 1);
        var request = java.net.http.HttpRequest
            .newBuilder(uri("/appointments"))
            .POST(java.net.http.HttpRequest.BodyPublishers.ofString(oversized))
            .build();

        var response = client.send(request, BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(413);
        assertThat(response.body()).contains(Router.REQUEST_TOO_LARGE);
    }

    @Test
    void shouldStreamBroadcastEventsToASubscribedClient() throws Exception {
        var request = java.net.http.HttpRequest.newBuilder(uri("/events")).GET().build();
        var response = client.send(request, BodyHandlers.ofInputStream());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type")).contains("text/event-stream;charset=UTF-8");

        awaitSubscription();
        hub.broadcast(SseEvent.named("appointmentScheduled", "{\"id\":\"A1\"}"));

        var reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8));

        assertThat(reader.readLine()).isEqualTo(SseEndpoint.HANDSHAKE.strip());
        assertThat(reader.readLine()).isEmpty();
        assertThat(reader.readLine()).isEqualTo("id: 1");
        assertThat(reader.readLine()).isEqualTo("event: appointmentScheduled");
        assertThat(reader.readLine()).isEqualTo("data: {\"id\":\"A1\"}");
    }

    @Test
    void shouldRejectANonGetRequestOnTheEventStream() throws Exception {
        var request = java.net.http.HttpRequest
            .newBuilder(uri("/events"))
            .POST(java.net.http.HttpRequest.BodyPublishers.noBody())
            .build();

        assertThat(client.send(request, BodyHandlers.ofString()).statusCode()).isEqualTo(405);
    }

    private void awaitSubscription() throws InterruptedException {
        for (var attempt = 0; attempt < 100 && hub.connectedCount() == 0; attempt++) {
            Thread.sleep(20);
        }

        assertThat(hub.connectedCount()).isEqualTo(1);
    }
}
