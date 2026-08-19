package atlas.presentation.appointments.presence;

import atlas.domain.sharedkernel.exceptions.FormatException;
import atlas.presentation.common.web.Json;
import atlas.presentation.sharedkernel.http.RouteHandler;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.Map;

public final class PresenceAccess {

    private static final Duration TIMEOUT = Duration.ofSeconds(2);

    private final URI baseUrl;
    private final URI activeSessionUrl;
    private final HttpClient client;

    public PresenceAccess(URI baseUrl) {
        this(baseUrl, HttpClient.newBuilder().connectTimeout(TIMEOUT).build());
    }

    PresenceAccess(URI baseUrl, HttpClient client) {
        this.baseUrl = baseUrl;
        this.activeSessionUrl = baseUrl.resolve("/sessions/active");
        this.client = client;
    }

    public atlas.presentation.sharedkernel.http.HttpResponse activeSession(
        atlas.presentation.sharedkernel.http.HttpRequest request) {
        var session = lookup();
        return switch (session.state()) {
            case ACTIVE -> atlas.presentation.sharedkernel.http.HttpResponse.json(200, session.body());
            case INACTIVE -> atlas.presentation.sharedkernel.http.HttpResponse.json(200, "null");
            case UNAVAILABLE -> unavailable();
        };
    }

    public atlas.presentation.sharedkernel.http.HttpResponse authenticationState(
        atlas.presentation.sharedkernel.http.HttpRequest request) {
        return forward("GET", "/authentication", "");
    }

    public atlas.presentation.sharedkernel.http.HttpResponse beginAuthentication(
        atlas.presentation.sharedkernel.http.HttpRequest request) {
        return forward("POST", "/authentication/challenges", request.body());
    }

    public atlas.presentation.sharedkernel.http.HttpResponse completeAuthentication(
        atlas.presentation.sharedkernel.http.HttpRequest request) {
        var challengeId = validId(request.pathParam("challengeId"), "L");
        return forward("POST", "/authentication/challenges/" + challengeId + "/complete", request.body());
    }

    public atlas.presentation.sharedkernel.http.HttpResponse profiles(
        atlas.presentation.sharedkernel.http.HttpRequest request) {
        return forward("GET", "/profiles", "");
    }

    public atlas.presentation.sharedkernel.http.HttpResponse enrollProfile(
        atlas.presentation.sharedkernel.http.HttpRequest request) {
        return forward("POST", "/profiles", request.body());
    }

    public atlas.presentation.sharedkernel.http.HttpResponse deleteProfile(
        atlas.presentation.sharedkernel.http.HttpRequest request) {
        return forward("DELETE", "/profiles/" + validId(request.pathParam("id"), "B"), "");
    }

    public atlas.presentation.sharedkernel.http.HttpResponse closeSession(
        atlas.presentation.sharedkernel.http.HttpRequest request) {
        return forward("DELETE", "/sessions/" + validId(request.pathParam("id"), "S"), "");
    }

    public atlas.presentation.sharedkernel.http.HttpResponse publishGesture(
        atlas.presentation.sharedkernel.http.HttpRequest request) {
        return forward("POST", "/interactions/gestures", request.body());
    }

    public RouteHandler protect(RouteHandler handler) {
        return request -> switch (lookup().state()) {
            case ACTIVE -> handler.handle(request);
            case INACTIVE -> authenticationRequired();
            case UNAVAILABLE -> unavailable();
        };
    }

    public boolean authenticated() {
        return lookup().state() == State.ACTIVE;
    }

    private atlas.presentation.sharedkernel.http.HttpResponse forward(String method, String path, String body) {
        var builder = HttpRequest.newBuilder(baseUrl.resolve(path))
            .timeout(TIMEOUT)
            .header("Accept", "application/json");
        var publisher = body.isBlank()
            ? HttpRequest.BodyPublishers.noBody()
            : HttpRequest.BodyPublishers.ofString(body);
        if (!body.isBlank()) {
            builder.header("Content-Type", "application/json");
        }

        try {
            var response = client.send(builder.method(method, publisher).build(), BodyHandlers.ofString());
            if (response.statusCode() == 204) {
                return atlas.presentation.sharedkernel.http.HttpResponse.noContent();
            }
            var responseBody = response.body() == null || response.body().isBlank() ? "null" : response.body();
            return atlas.presentation.sharedkernel.http.HttpResponse.json(response.statusCode(), responseBody);
        } catch (IOException e) {
            return unavailable();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return unavailable();
        }
    }

    private static String validId(String value, String prefix) {
        if (!value.matches(prefix + "[0-9]{8}")) {
            throw new FormatException("Invalid Presence identifier.");
        }
        return value;
    }

    private Session lookup() {
        var request = HttpRequest.newBuilder(activeSessionUrl)
            .timeout(TIMEOUT)
            .header("Accept", "application/json")
            .GET()
            .build();
        try {
            var response = client.send(request, BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return Session.unavailable();
            }

            var body = response.body() == null ? "" : response.body().trim();
            if (body.isEmpty() || "null".equals(body)) {
                return Session.inactive();
            }

            var value = Json.parse(body);
            return "ACTIVE".equals(value.get("status")) ? Session.active(body) : Session.inactive();
        } catch (IOException | FormatException e) {
            return Session.unavailable();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Session.unavailable();
        }
    }

    private static atlas.presentation.sharedkernel.http.HttpResponse authenticationRequired() {
        return atlas.presentation.sharedkernel.http.HttpResponse.json(401, Json.write(Map.of(
            "code", "Presence.AuthenticationRequired",
            "message", "An active Presence session is required.")));
    }

    private static atlas.presentation.sharedkernel.http.HttpResponse unavailable() {
        return atlas.presentation.sharedkernel.http.HttpResponse.json(503, Json.write(Map.of(
            "code", "Presence.Unavailable",
            "message", "Presence is not available.")));
    }

    private enum State {
        ACTIVE,
        INACTIVE,
        UNAVAILABLE
    }

    private record Session(State state, String body) {

        private static Session active(String body) {
            return new Session(State.ACTIVE, body);
        }

        private static Session inactive() {
            return new Session(State.INACTIVE, "null");
        }

        private static Session unavailable() {
            return new Session(State.UNAVAILABLE, "null");
        }
    }
}
