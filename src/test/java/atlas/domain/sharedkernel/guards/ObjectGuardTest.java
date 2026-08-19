package atlas.domain.sharedkernel.guards;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import atlas.domain.sharedkernel.exceptions.GuardException;
import org.junit.jupiter.api.Test;

class ObjectGuardTest {

    @Test
    void shouldReturnValueWhenNotNull() {
        assertThat(ObjectGuard.notNull("value", "parameter")).isEqualTo("value");
    }

    @Test
    void shouldThrowWhenNull() {
        assertThatThrownBy(() -> ObjectGuard.notNull(null, "organizerId"))
            .isInstanceOf(GuardException.class)
            .hasMessageContaining("organizerId")
            .hasMessageContaining("cannot be null");
    }

    @Test
    void shouldCarryParameterNameWhenThrown() {
        assertThatThrownBy(() -> ObjectGuard.notNull(null, "organizerId"))
            .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.type(GuardException.class))
            .extracting(GuardException::parameterName)
            .isEqualTo("organizerId");
    }
}
