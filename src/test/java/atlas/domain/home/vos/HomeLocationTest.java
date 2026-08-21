package atlas.domain.home.vos;

import static org.assertj.core.api.Assertions.assertThat;

import atlas.domain.home.HomeErrors;
import org.junit.jupiter.api.Test;

class HomeLocationTest {

    @Test
    void shouldCreateAValidatedLocationAndTimeZone() {
        var result = HomeLocation.create(" Las Palmas ", 28.1235, -15.4363, "Atlantic/Canary");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.value().name()).isEqualTo("Las Palmas");
        assertThat(result.value().timeZone().getId()).isEqualTo("Atlantic/Canary");
    }

    @Test
    void shouldRejectCoordinatesOutsideTheEarth() {
        var result = HomeLocation.create("Las Palmas", 91, -15.4363, "Atlantic/Canary");

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error()).isEqualTo(HomeErrors.INVALID_COORDINATES);
    }

    @Test
    void shouldRejectAnUnknownTimeZone() {
        var result = HomeLocation.create("Las Palmas", 28.1235, -15.4363, "Atlantic/Atlantis");

        assertThat(result.isFailure()).isTrue();
        assertThat(result.error()).isEqualTo(HomeErrors.INVALID_TIME_ZONE);
    }
}
