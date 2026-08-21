package atlas.presentation.home.web;

import atlas.domain.home.enums.NewsCategory;
import atlas.domain.sharedkernel.exceptions.FormatException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Values {

    private Values() {}

    public static String text(Map<String, Object> body, String field) {
        var value = body.get(field);
        if (value == null) {
            return null;
        }
        if (!(value instanceof String text)) {
            throw new FormatException("'" + field + "' must be a string.");
        }

        return text;
    }

    public static double number(Map<String, Object> body, String field) {
        var value = body.get(field);
        if (!(value instanceof Number number)) {
            throw new FormatException("'" + field + "' must be a number.");
        }

        return number.doubleValue();
    }

    public static Set<NewsCategory> categories(Map<String, Object> body, String field) {
        var value = body.get(field);
        if (!(value instanceof List<?> values)) {
            throw new FormatException("'" + field + "' must be an array of news categories.");
        }

        var categories = new LinkedHashSet<NewsCategory>();
        for (var element : values) {
            if (!(element instanceof String text)) {
                throw new FormatException("'" + field + "' must be an array of news categories.");
            }
            try {
                categories.add(NewsCategory.valueOf(text.toUpperCase()));
            } catch (IllegalArgumentException exception) {
                throw new FormatException("'" + field + "' contains an unknown news category.");
            }
        }

        return categories;
    }

    public static Set<NewsCategory> categories(String query) {
        if (query == null || query.isBlank()) {
            return Set.of(NewsCategory.values());
        }

        var values = new LinkedHashSet<NewsCategory>();
        for (var value : query.split(",")) {
            try {
                values.add(NewsCategory.valueOf(value.trim().toUpperCase()));
            } catch (IllegalArgumentException exception) {
                throw new FormatException("'categories' contains an unknown news category.");
            }
        }

        return values;
    }
}
