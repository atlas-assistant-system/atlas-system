package atlas.domain.training.enums;

import atlas.domain.training.vos.Effort;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Qué se mide en un ejercicio y, por tanto, qué significa progresar en él. Es la única
 * definición de la comparación en el contexto: la mejor marca y el volumen salen de aquí
 * y de ningún otro sitio, para que no puedan divergir.
 *
 * <p>
 * En {@code LOAD} la mejor marca es la mayor carga, sin normalizar por repeticiones:
 * 80×5 gana a 70×8. Normalizar exigiría un 1RM estimado, que es una estimación disfrazada
 * de medida. Por eso la carga siempre se enseña con sus reps al lado.
 */
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

    /** Empate: gana la primera. Batir una marca exige superarla, no igualarla. */
    public Optional<Effort> bestOf(List<Effort> sets) {
        return sets.stream().max(Comparator.comparingLong(this::scoreOf));
    }

    public String label() {
        return label;
    }
}
