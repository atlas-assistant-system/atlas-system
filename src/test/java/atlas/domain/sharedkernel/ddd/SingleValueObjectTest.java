package atlas.domain.sharedkernel.ddd;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SingleValueObjectTest {

    @Test
    void shouldExposeValueThroughInterfaceWhenImplementedByRecord() {
        SingleValueObject<String> title = new SampleTitle("checkup");

        assertThat(title.value()).isEqualTo("checkup");
    }

    @Test
    void shouldBeEqualWhenValuesMatch() {
        assertThat(new SampleTitle("checkup")).isEqualTo(new SampleTitle("checkup"));
        assertThat(new SampleTitle("checkup")).isNotEqualTo(new SampleTitle("other"));
    }
}
