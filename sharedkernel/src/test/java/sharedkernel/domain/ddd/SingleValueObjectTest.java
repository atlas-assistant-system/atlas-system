package sharedkernel.domain.ddd;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SingleValueObjectTest {

    private record SampleTitle(String value) implements SingleValueObject<String> {}

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
