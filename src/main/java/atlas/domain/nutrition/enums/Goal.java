package atlas.domain.nutrition.enums;

import atlas.domain.nutrition.vos.Weight;

public enum Goal {

    LOSE("Perder peso") {

        @Override
        public boolean isReached(Weight current, Weight target) {
            return current.grams() <= target.grams();
        }
    },
    MAINTAIN("Mantenerme") {

        @Override
        public boolean isReached(Weight current, Weight target) {
            return Math.abs(current.gramsTo(target)) <= TOLERANCE_GRAMS;
        }
    },
    GAIN("Ganar peso") {

        @Override
        public boolean isReached(Weight current, Weight target) {
            return current.grams() >= target.grams();
        }
    };

    public static final int TOLERANCE_GRAMS = 500;

    private final String label;

    Goal(String label) {
        this.label = label;
    }

    public static Goal of(Weight start, Weight target) {
        var difference = start.gramsTo(target);

        if (Math.abs(difference) <= TOLERANCE_GRAMS) {
            return MAINTAIN;
        }

        return difference < 0 ? LOSE : GAIN;
    }

    public abstract boolean isReached(Weight current, Weight target);

    public String label() {
        return label;
    }
}
