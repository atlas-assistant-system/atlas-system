package sharedkernel.domain.ddd;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import sharedkernel.domain.exceptions.GuardException;

class EntityTest {

    private record SampleId(long value) {}

    private static final class SampleEntity extends Entity<SampleId> {

        SampleEntity(SampleId id) {
            super(id);
        }
    }

    private static final class OtherEntity extends Entity<SampleId> {

        OtherEntity(SampleId id) {
            super(id);
        }
    }

    @Test
    void shouldExposeIdWhenConstructed() {
        var entity = new SampleEntity(new SampleId(1));

        assertThat(entity.id()).isEqualTo(new SampleId(1));
    }

    @Test
    void shouldBeEqualWhenIdsMatch() {
        var first = new SampleEntity(new SampleId(7));
        var second = new SampleEntity(new SampleId(7));

        assertThat(first).isEqualTo(second);
        assertThat(first).hasSameHashCodeAs(second);
    }

    @Test
    void shouldNotBeEqualWhenIdsDiffer() {
        var first = new SampleEntity(new SampleId(7));
        var second = new SampleEntity(new SampleId(8));

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void shouldNotBeEqualWhenClassesDiffer() {
        var entity = new SampleEntity(new SampleId(7));
        var other = new OtherEntity(new SampleId(7));

        assertThat(entity).isNotEqualTo(other);
    }

    @Test
    void shouldThrowWhenConstructedWithNullId() {
        assertThatThrownBy(() -> new SampleEntity(null))
            .isInstanceOf(GuardException.class);
    }
}
