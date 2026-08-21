package atlas.application.home.mappers;

import atlas.application.home.dto.HomeProfileDto;
import atlas.domain.home.HomeProfile;

public final class HomeMapper {

    private HomeMapper() {}

    public static HomeProfileDto toDto(HomeProfile profile) {
        var location = profile.location();

        return new HomeProfileDto(
            profile.id().value(),
            location.name(),
            location.latitude(),
            location.longitude(),
            location.timeZone().getId(),
            profile.newsCategories());
    }
}
