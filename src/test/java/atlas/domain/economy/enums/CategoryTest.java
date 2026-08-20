package atlas.domain.economy.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class CategoryTest {

    @Test
    void shouldLabelIncomeAsAnIncomeCategory() {
        assertThat(Category.INCOME.kind()).isEqualTo(MovementKind.INCOME);
    }

    @ParameterizedTest
    @EnumSource(value = Category.class, names = "INCOME", mode = EnumSource.Mode.EXCLUDE)
    void shouldTreatEveryOtherCategoryAsSpending(Category category) {
        assertThat(category.kind()).isEqualTo(MovementKind.EXPENSE);
    }

    @Test
    void shouldMatchTheKindItBelongsTo() {
        assertThat(Category.FOOD.matches(MovementKind.EXPENSE)).isTrue();
        assertThat(Category.FOOD.matches(MovementKind.INCOME)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(Category.class)
    void shouldCarryALabelAndAnIconForTheMirror(Category category) {
        assertThat(category.label()).isNotBlank();
        assertThat(category.icon()).isNotBlank();
    }

    @ParameterizedTest
    @EnumSource(Category.class)
    void shouldSurviveARoundTripThroughItsStoredName(Category category) {
        assertThat(Category.valueOf(category.name())).isEqualTo(category);
    }
}
