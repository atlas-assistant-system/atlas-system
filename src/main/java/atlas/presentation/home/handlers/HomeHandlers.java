package atlas.presentation.home.handlers;

import atlas.application.home.commands.configurehome.ConfigureHomeCommand;
import atlas.application.home.dto.HomeProfileDto;
import atlas.application.home.queries.gethomeprofile.GetHomeProfileQuery;
import atlas.application.sharedkernel.cqrs.CommandBus;
import atlas.application.sharedkernel.cqrs.QueryBus;
import atlas.domain.home.HomeErrors;
import atlas.domain.home.HomeProfileId;
import atlas.domain.sharedkernel.results.Result;
import atlas.presentation.common.web.Json;
import atlas.presentation.home.requests.ConfigureHomeRequest;
import atlas.presentation.home.responses.HomeResponses;
import atlas.presentation.sharedkernel.http.HttpRequest;
import atlas.presentation.sharedkernel.http.HttpResponse;
import java.util.function.Predicate;

public final class HomeHandlers {

    private final CommandBus commands;
    private final QueryBus queries;
    private final Predicate<HomeProfileId> mayAccess;

    public HomeHandlers(CommandBus commands, QueryBus queries, Predicate<HomeProfileId> mayAccess) {
        this.commands = commands;
        this.queries = queries;
        this.mayAccess = mayAccess;
    }

    public HttpResponse configure(HttpRequest request) {
        var profileId = profileId(request);
        if (!mayAccess.test(profileId)) {
            return HttpResponse.error(HomeErrors.ACCESS_DENIED);
        }
        var body = ConfigureHomeRequest.from(Json.parse(request.body()));
        Result<HomeProfileDto> result = commands.dispatch(new ConfigureHomeCommand(
            profileId,
            body.locationName(),
            body.latitude(),
            body.longitude(),
            body.timeZone(),
            body.newsCategories()));

        return response(result);
    }

    public HttpResponse get(HttpRequest request) {
        var profileId = profileId(request);
        if (!mayAccess.test(profileId)) {
            return HttpResponse.error(HomeErrors.ACCESS_DENIED);
        }

        return response(queries.dispatch(new GetHomeProfileQuery(profileId)));
    }

    private static HttpResponse response(Result<HomeProfileDto> result) {
        if (result.isFailure()) {
            return HttpResponse.error(result.error());
        }

        return HttpResponse.ok(Json.write(HomeResponses.profile(result.value())));
    }

    private static HomeProfileId profileId(HttpRequest request) {
        return HomeProfileId.of(request.pathParam("profileId"));
    }
}
