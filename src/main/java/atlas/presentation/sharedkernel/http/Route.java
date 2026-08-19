package atlas.presentation.sharedkernel.http;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

record Route(String method, List<String> pattern, RouteHandler handler) {

    boolean matchesPath(List<String> segments) {
        if (segments.size() != pattern.size()) {
            return false;
        }

        for (var index = 0; index < pattern.size(); index++) {
            var expected = pattern.get(index);

            if (!isParameter(expected) && !expected.equals(segments.get(index))) {
                return false;
            }
        }

        return true;
    }

    Map<String, String> extractParams(List<String> segments) {
        var params = new LinkedHashMap<String, String>();

        for (var index = 0; index < pattern.size(); index++) {
            var expected = pattern.get(index);

            if (isParameter(expected)) {
                params.put(expected.substring(1, expected.length() - 1), segments.get(index));
            }
        }

        return params;
    }

    private static boolean isParameter(String segment) {
        return segment.length() > 2 && segment.charAt(0) == '{' && segment.charAt(segment.length() - 1) == '}';
    }
}
