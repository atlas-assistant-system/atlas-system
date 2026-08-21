package atlas.application.home.dto;

import atlas.domain.home.enums.NewsCategory;
import java.util.Set;

public record HomeProfileDto(
    String profileId,
    String locationName,
    double latitude,
    double longitude,
    String timeZone,
    Set<NewsCategory> newsCategories) {}
