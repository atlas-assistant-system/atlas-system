package atlas.application.home.commands.configurehome;

import atlas.application.home.dto.HomeProfileDto;
import atlas.application.sharedkernel.cqrs.Command;
import atlas.domain.home.HomeProfileId;
import atlas.domain.home.enums.NewsCategory;
import atlas.domain.sharedkernel.results.Result;
import java.util.Set;

public record ConfigureHomeCommand(
    HomeProfileId profileId,
    String locationName,
    double latitude,
    double longitude,
    String timeZone,
    Set<NewsCategory> newsCategories) implements Command<Result<HomeProfileDto>> {}
