package atlas.presentation.sharedkernel.http;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

final class PathSegments {

    private PathSegments() {}

    static List<String> of(String path) {
        var segments = new ArrayList<String>();

        for (var segment : path.split("/")) {
            if (!segment.isEmpty()) {
                segments.add(URLDecoder.decode(segment, StandardCharsets.UTF_8));
            }
        }

        return List.copyOf(segments);
    }
}
