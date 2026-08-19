package atlas.presentation.presence.handlers;

import atlas.application.presence.commands.closesession.CloseSessionCommand;
import atlas.application.presence.commands.refreshsession.RefreshSessionCommand;
import atlas.application.presence.queries.getactivesession.GetActiveSessionQuery;
import atlas.domain.presence.PresenceErrors;
import atlas.domain.presence.SessionId;
import atlas.presentation.common.web.Json;
import atlas.presentation.common.web.Values;
import java.util.Map;
import java.util.Set;
import sharedkernel.application.cqrs.CommandBus;
import sharedkernel.application.cqrs.QueryBus;
import sharedkernel.domain.exceptions.FormatException;
import sharedkernel.presentation.http.HttpRequest;
import sharedkernel.presentation.http.HttpResponse;
import sharedkernel.presentation.sse.SseEvent;
import sharedkernel.presentation.sse.SseHub;

public final class InteractionHandlers {

    private static final Set<String> GESTURES = Set.of(
        "FIST", "OPEN_PALM", "PALM_DOWN", "PALM_LEFT", "PALM_RIGHT", "PALM_UP",
        "PINCH", "POINT", "THUMBS_UP", "VICTORY");

    private final CommandBus commands;
    private final QueryBus queries;
    private final SseHub hub;

    public InteractionHandlers(CommandBus commands, QueryBus queries, SseHub hub) {
        this.commands = commands;
        this.queries = queries;
        this.hub = hub;
    }

    public HttpResponse gesture(HttpRequest request) {
        var active = queries.dispatch(new GetActiveSessionQuery());
        if (active.isFailure()) {
            return HttpResponse.error(active.error());
        }
        if (active.value().isEmpty()) {
            return HttpResponse.error(PresenceErrors.INTERACTION_REQUIRES_SESSION);
        }

        var body = Json.parse(request.body());
        var type = Values.text(body, "type");
        if (!GESTURES.contains(type)) {
            throw new FormatException("'type' must be a supported hand gesture.");
        }
        var confidence = confidence(body);
        var handIndex = handIndex(body);
        var observedAt = Values.instant(body, "observedAt");
        var session = active.value().orElseThrow();
        var sessionId = SessionId.parse(session.id());
        if ("VICTORY".equals(type)) {
            var closed = commands.dispatch(new CloseSessionCommand(sessionId));
            if (closed.isFailure()) {
                return HttpResponse.error(closed.error());
            }
        } else {
            var refreshed = commands.dispatch(new RefreshSessionCommand(sessionId));
            if (refreshed.isFailure()) {
                return HttpResponse.error(refreshed.error());
            }
        }

        hub.broadcast(SseEvent.named("gestureDetected", Json.write(Map.of(
            "type", type,
            "handIndex", handIndex,
            "confidence", confidence,
            "observedAt", observedAt.toString(),
            "sessionId", session.id(),
            "profileId", session.profileId()))));

        return HttpResponse.noContent();
    }

    private static double confidence(Map<String, Object> body) {
        if (!(body.get("confidence") instanceof Number number)) {
            throw new FormatException("'confidence' must be a number between 0 and 1.");
        }
        var value = number.doubleValue();
        if (!Double.isFinite(value) || value < 0 || value > 1) {
            throw new FormatException("'confidence' must be a number between 0 and 1.");
        }
        return value;
    }

    private static int handIndex(Map<String, Object> body) {
        if (!(body.get("handIndex") instanceof Number number)) {
            throw new FormatException("'handIndex' must be a non-negative whole number.");
        }
        var value = number.doubleValue();
        if (!Double.isFinite(value) || value < 0 || value != Math.rint(value)) {
            throw new FormatException("'handIndex' must be a non-negative whole number.");
        }
        return (int) value;
    }
}
