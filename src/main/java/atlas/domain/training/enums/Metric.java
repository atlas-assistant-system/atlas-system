package atlas.domain.training.enums;

import atlas.domain.training.vos.Effort;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;


public enum Metric {

    LOAD("Carga") {

        @Override
        public long scoreOf(Effort effort) {
            return effort.loadGrams();
        }

        @Override
        public long volumeOf(Effort effort) {
            return (long) effort.loadGrams() * effort.reps();
        }
    },
    REPS("Repeticiones") {

        @Override
        public long scoreOf(Effort effort) {
            return effort.reps();
        }

        @Override
        public long volumeOf(Effort effort) {
            return effort.reps();
        }
    },
    TIME("Tiempo") {

        @Override
        public long scoreOf(Effort effort) {
            return effort.seconds();
        }

        @Override
        public long volumeOf(Effort effort) {
            return effort.seconds();
        }
    },
    DISTANCE("Distancia") {

        @Override
        public long scoreOf(Effort effort) {
            return effort.meters();
        }

        @Override
        public long volumeOf(Effort effort) {
            return effort.meters();
        }
    };

    private final String label;

    Metric(String label) {
        this.label = label;
    }

    public abstract long scoreOf(Effort effort);

    public abstract long volumeOf(Effort effort);

    
    public Optional<Effort> bestOf(List<Effort> sets) {
        return sets.stream().max(Comparator.comparingLong(this::scoreOf));
    }

    public String label() {
        return label;
    }
}
