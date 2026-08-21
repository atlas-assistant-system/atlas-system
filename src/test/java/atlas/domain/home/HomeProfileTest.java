package atlas.domain.home;

import static org.assertj.core.api.Assertions.assertThat;

import atlas.domain.home.enums.NewsCategory;
import atlas.domain.home.events.HomeConfiguredEvent;
import atlas.domain.home.vos.HomeLocation;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;

class HomeProfileTest {

    private static final Instant NOW = Instant.parse("2026-08-21T10:00:00Z");
    private static final HomeLocation LOCATION =
        HomeLocation.create("Las Palmas", 28.1235, -15.4363, "Atlantic/Canary").value();

    @Test
    void shouldConfigureAHomeForAnExternalProfile() {
        var result = HomeProfile.configure(
            HomeProfileId.of("P00000001"), LOCATION, Set.of(NewsCategory.AI, NewsCategory.DEVELOPMENT), NOW);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.value().newsCategories()).containsExactlyInAnyOrder(
            NewsCategory.AI, NewsCategory.DEVELOPMENT);
        assertThat(result.value().pendingEvents()).containsExactly(
            new HomeConfiguredEvent(HomeProfileId.of("P00000001"), NOW));
    }

    @Test
    void shouldRequireAtLeastOneNewsCategory() {
        var result = HomeProfile.configure(HomeProfileId.of("P00000001"), LOCATION, Set.of(), NOW);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error()).isEqualTo(HomeErrors.NEWS_CATEGORIES_REQUIRED);
    }
}
