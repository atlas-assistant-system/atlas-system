package atlas.presentation.home.requests;

import atlas.domain.home.enums.NewsCategory;
import atlas.presentation.home.web.Values;
import java.util.Map;
import java.util.Set;

public record ConfigureHomeRequest(
    String locationName,
    double latitude,
    double longitude,
    String timeZone,
    Set<NewsCategory> newsCategories) {

    public static ConfigureHomeRequest from(Map<String, Object> body) {
        return new ConfigureHomeRequest(
            Values.text(body, "locationName"),
            Values.number(body, "latitude"),
            Values.number(body, "longitude"),
            Values.text(body, "timeZone"),
            Values.categories(body, "newsCategories"));
    }
}
