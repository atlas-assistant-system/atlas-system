package atlas.presentation.presence.handlers;

import atlas.application.presence.commands.addfacetemplate.AddFaceTemplateCommand;
import atlas.application.presence.commands.beginauthentication.BeginAuthenticationCommand;
import atlas.application.presence.commands.closesession.CloseSessionCommand;
import atlas.application.presence.commands.completeauthentication.CompleteAuthenticationCommand;
import atlas.application.presence.commands.deleteprofile.DeleteProfileCommand;
import atlas.application.presence.commands.enrollprofile.EnrollProfileCommand;
import atlas.application.presence.commands.refreshsession.RefreshSessionCommand;
import atlas.application.presence.commands.removefacetemplate.RemoveFaceTemplateCommand;
import atlas.application.presence.dto.ProfileDto;
import atlas.application.presence.queries.getactivesession.GetActiveSessionQuery;
import atlas.application.presence.queries.getauthenticationstate.GetAuthenticationStateQuery;
import atlas.application.presence.queries.getprofile.GetProfileQuery;
import atlas.application.presence.queries.listauthenticationattempts.ListAuthenticationAttemptsQuery;
import atlas.application.presence.queries.listprofiles.ListProfilesQuery;
import atlas.domain.presence.BiometricProfileId;
import atlas.domain.presence.LivenessChallengeId;
import atlas.domain.presence.PresenceErrors;
import atlas.domain.presence.SessionId;
import atlas.domain.presence.entities.FaceTemplateId;
import atlas.presentation.common.web.Json;
import atlas.presentation.presence.requests.PresenceRequests;
import atlas.presentation.presence.responses.PresenceResponses;
import atlas.presentation.presence.web.Values;
import java.util.UUID;
import java.util.function.Function;
import sharedkernel.application.cqrs.CommandBus;
import sharedkernel.application.cqrs.QueryBus;
import sharedkernel.application.paging.PageRequest;
import sharedkernel.domain.exceptions.FormatException;
import sharedkernel.domain.results.Result;
import sharedkernel.presentation.http.HttpRequest;
import sharedkernel.presentation.http.HttpResponse;

public final class PresenceHandlers {

    private final CommandBus commands;
    private final QueryBus queries;
    private final boolean maintenanceMode;

    public PresenceHandlers(CommandBus commands, QueryBus queries, boolean maintenanceMode) {
        this.commands = commands;
        this.queries = queries;
        this.maintenanceMode = maintenanceMode;
    }

    public HttpResponse enrollProfile(HttpRequest request) {
        var denied = managementDenied();
        if (denied != null) {
            return denied;
        }
        var body = PresenceRequests.enrollProfile(Json.parse(request.body()));
        Result<ProfileDto> result = commands.dispatch(
            new EnrollProfileCommand(body.displayName(), body.modelVersion(), body.descriptor()));
        if (result.isFailure()) {
            return HttpResponse.error(result.error());
        }
        return HttpResponse.created("/profiles/" + result.value().id(),
            Json.write(PresenceResponses.profile(result.value())));
    }

    public HttpResponse listProfiles(HttpRequest request) {
        var denied = managementDenied();
        if (denied != null) {
            return denied;
        }
        return ok(queries.dispatch(new ListProfilesQuery()), PresenceResponses::profileSummaries);
    }

    public HttpResponse getProfile(HttpRequest request) {
        var denied = managementDenied();
        if (denied != null) {
            return denied;
        }
        return ok(queries.dispatch(new GetProfileQuery(profileId(request))), PresenceResponses::profile);
    }

    public HttpResponse deleteProfile(HttpRequest request) {
        var denied = managementDenied();
        if (denied != null) {
            return denied;
        }
        return noContent(commands.dispatch(new DeleteProfileCommand(profileId(request))));
    }

    public HttpResponse addFaceTemplate(HttpRequest request) {
        var denied = managementDenied();
        if (denied != null) {
            return denied;
        }
        var body = PresenceRequests.faceTemplate(Json.parse(request.body()));
        return ok(
            commands.dispatch(new AddFaceTemplateCommand(profileId(request), body.modelVersion(), body.descriptor())),
            PresenceResponses::profile);
    }

    public HttpResponse removeFaceTemplate(HttpRequest request) {
        var denied = managementDenied();
        if (denied != null) {
            return denied;
        }
        return noContent(commands.dispatch(
            new RemoveFaceTemplateCommand(profileId(request), templateId(request))));
    }

    public HttpResponse authenticationState(HttpRequest request) {
        return ok(queries.dispatch(new GetAuthenticationStateQuery()), state -> {
            var response = PresenceResponses.authenticationState(state);
            response.put("maintenanceMode", maintenanceMode);
            return response;
        });
    }

    public HttpResponse beginAuthentication(HttpRequest request) {
        var result = commands.dispatch(new BeginAuthenticationCommand());
        if (result.isFailure()) {
            return HttpResponse.error(result.error());
        }
        return HttpResponse.created(
            "/authentication/challenges/" + result.value().challengeId(),
            Json.write(PresenceResponses.challenge(result.value())));
    }

    public HttpResponse completeAuthentication(HttpRequest request) {
        var body = PresenceRequests.completeAuthentication(Json.parse(request.body()));
        return ok(
            commands.dispatch(new CompleteAuthenticationCommand(
                challengeId(request),
                body.modelVersion(),
                body.descriptor(),
                body.observedType(),
                body.nonce(),
                body.capturedAt())),
            PresenceResponses::session);
    }

    public HttpResponse listAuthenticationAttempts(HttpRequest request) {
        var denied = managementDenied();
        if (denied != null) {
            return denied;
        }
        var page = PageRequest.of(
            Values.parseInteger(request.queryParam("page"), "page", 1),
            Values.parseInteger(request.queryParam("pageSize"), "pageSize", PageRequest.DEFAULT_PAGE_SIZE));
        return ok(
            queries.dispatch(new ListAuthenticationAttemptsQuery(page)),
            PresenceResponses::attempts);
    }

    public HttpResponse activeSession(HttpRequest request) {
        return ok(queries.dispatch(new GetActiveSessionQuery()), PresenceResponses::optionalSession);
    }

    public HttpResponse refreshSession(HttpRequest request) {
        return ok(
            commands.dispatch(new RefreshSessionCommand(sessionId(request))),
            PresenceResponses::session);
    }

    public HttpResponse closeSession(HttpRequest request) {
        return noContent(commands.dispatch(new CloseSessionCommand(sessionId(request))));
    }

    private HttpResponse managementDenied() {
        if (maintenanceMode) {
            return null;
        }

        var active = queries.dispatch(new GetActiveSessionQuery());
        if (active.isFailure()) {
            return HttpResponse.error(active.error());
        }
        return active.value().isPresent() ? null : HttpResponse.error(PresenceErrors.INTERACTION_REQUIRES_SESSION);
    }

    private static <T> HttpResponse ok(Result<T> result, Function<T, Object> response) {
        if (result.isFailure()) {
            return HttpResponse.error(result.error());
        }
        return HttpResponse.ok(Json.write(response.apply(result.value())));
    }

    private static HttpResponse noContent(Result<Void> result) {
        return result.isFailure() ? HttpResponse.error(result.error()) : HttpResponse.noContent();
    }

    private static BiometricProfileId profileId(HttpRequest request) {
        return BiometricProfileId.parse(request.pathParam("id"));
    }

    private static LivenessChallengeId challengeId(HttpRequest request) {
        return LivenessChallengeId.parse(request.pathParam("challengeId"));
    }

    private static SessionId sessionId(HttpRequest request) {
        return SessionId.parse(request.pathParam("id"));
    }

    private static FaceTemplateId templateId(HttpRequest request) {
        var value = request.pathParam("templateId");
        try {
            var uuid = UUID.fromString(value);
            if (!uuid.toString().equalsIgnoreCase(value)) {
                throw new IllegalArgumentException();
            }
            return FaceTemplateId.of(uuid);
        } catch (IllegalArgumentException e) {
            throw new FormatException("'templateId' must be a UUID.");
        }
    }
}
