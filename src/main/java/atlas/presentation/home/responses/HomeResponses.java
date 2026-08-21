package atlas.presentation.home.responses;

import atlas.application.home.dto.HomeProfileDto;
import java.util.LinkedHashMap;
import java.util.Map;

public final class HomeResponses {

    private HomeResponses() {}

    public static Map<String, Object> profile(HomeProfileDto dto) {
        var location = new LinkedHashMap<String, Object>();
        location.put("name", dto.locationName());
        location.put("latitude", dto.latitude());
        location.put("longitude", dto.longitude());
        location.put("timeZone", dto.timeZone());

        var body = new LinkedHashMap<String, Object>();
        body.put("profileId", dto.profileId());
        body.put("location", location);
        body.put("newsCategories", dto.newsCategories().stream().sorted().map(Enum::name).toList());

        return body;
    }
}
