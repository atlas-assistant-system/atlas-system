package atlas.domain.sharedkernel.guards;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import atlas.domain.sharedkernel.exceptions.GuardException;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class CollectionGuardTest {

    @Test
    void shouldReturnCollectionWhenNotEmpty() {
        assertThat(CollectionGuard.notEmpty(List.of("a"), "attendees")).containsExactly("a");
    }

    @Test
    void shouldThrowWhenEmpty() {
        assertThatThrownBy(() -> CollectionGuard.notEmpty(List.of(), "attendees"))
            .isInstanceOf(GuardException.class)
            .hasMessageContaining("cannot be empty");
    }

    @Test
    void shouldThrowWhenNull() {
        assertThatThrownBy(() -> CollectionGuard.notEmpty(null, "attendees"))
            .isInstanceOf(GuardException.class);
    }

    @Test
    void shouldThrowWhenAnyElementIsNull() {
        assertThatThrownBy(() -> CollectionGuard.noNullElements(Arrays.asList("a", null), "attendees"))
            .isInstanceOf(GuardException.class)
            .hasMessageContaining("cannot contain null elements");
    }
}
