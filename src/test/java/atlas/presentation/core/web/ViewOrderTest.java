package atlas.presentation.core.web;

import static org.assertj.core.api.Assertions.assertThat;

import atlas.presentation.common.web.StaticResources;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;


class ViewOrderTest {

    private static final Pattern NAV_TAB = Pattern.compile("data-view=\"([a-z]+)\"");
    private static final Pattern VIEW_ORDER = Pattern.compile("const VIEWS = \\[([^\\]]+)\\]");

    @Test
    void shouldLetTheSwipeGestureReachEveryTab() {
        assertThat(swipeOrder()).containsExactlyElementsOf(navigationTabs());
    }

    private static List<String> navigationTabs() {
        var page = StaticResources.read("/web-core/app.html");
        var tabs = NAV_TAB.matcher(page).results().map(match -> match.group(1)).toList();

        assertThat(tabs).isNotEmpty();

        return tabs;
    }

    private static List<String> swipeOrder() {
        var script = StaticResources.read("/web-core/app.js");
        var declaration = VIEW_ORDER.matcher(script);

        assertThat(declaration.find()).isTrue();

        return Arrays.stream(declaration.group(1).split(","))
            .map(view -> view.trim().replace("'", ""))
            .filter(view -> !view.isEmpty())
            .toList();
    }
}
