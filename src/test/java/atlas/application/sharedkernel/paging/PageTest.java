package atlas.application.sharedkernel.paging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PageTest {

    @ParameterizedTest
    @CsvSource({"0, 20", "-5, 20", "1, 1", "100, 100", "101, 100", "5000, 100"})
    void shouldClampPageSizeWhenOutOfBounds(int requested, int expected) {
        assertThat(PageRequest.of(1, requested).pageSize()).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({"0, 1", "-3, 1", "1, 1", "7, 7"})
    void shouldClampPageNumberWhenBelowOne(int requested, int expected) {
        assertThat(PageRequest.of(requested, 20).pageNumber()).isEqualTo(expected);
    }

    @Test
    void shouldComputeOffsetFromPageNumberAndSize() {
        assertThat(PageRequest.of(1, 20).offset()).isZero();
        assertThat(PageRequest.of(3, 20).offset()).isEqualTo(40);
    }

    @ParameterizedTest
    @CsvSource({"0, 20, 0", "1, 20, 1", "20, 20, 1", "21, 20, 2", "41, 20, 3"})
    void shouldComputeTotalPagesWhenCounted(long totalCount, int pageSize, int expectedPages) {
        var page = new Page<>(List.of(), 1, pageSize, totalCount);

        assertThat(page.totalPages()).isEqualTo(expectedPages);
    }

    @Test
    void shouldReportNavigationWhenInTheMiddle() {
        var page = new Page<>(List.of("a"), 2, 20, 60);

        assertThat(page.hasPrevious()).isTrue();
        assertThat(page.hasNext()).isTrue();
    }

    @Test
    void shouldNotHaveNextWhenOnLastPage() {
        var page = new Page<>(List.of("a"), 3, 20, 60);

        assertThat(page.hasNext()).isFalse();
        assertThat(page.hasPrevious()).isTrue();
    }

    @Test
    void shouldKeepPagingWhenMapped() {
        var page = new Page<>(List.of(1, 2), 2, 20, 60).map(String::valueOf);

        assertThat(page.items()).containsExactly("1", "2");
        assertThat(page.pageNumber()).isEqualTo(2);
        assertThat(page.totalCount()).isEqualTo(60);
    }

    @Test
    void shouldRejectExternalMutationWhenExposingItems() {
        var page = Page.of(List.of("a"), PageRequest.first(), 1);

        assertThat(page.items()).isUnmodifiable();
    }

    @Test
    void shouldBeEmptyWhenNothingFound() {
        var page = Page.empty(PageRequest.first());

        assertThat(page.isEmpty()).isTrue();
        assertThat(page.totalPages()).isZero();
        assertThat(page.hasNext()).isFalse();
    }
}
