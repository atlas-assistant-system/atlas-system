package atlas.domain.training.vos;

import atlas.domain.sharedkernel.ddd.ValueObject;
import atlas.domain.sharedkernel.guards.NumberGuard;
import atlas.domain.sharedkernel.results.Result;
import atlas.domain.training.TrainingErrors;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Lo que cuesta una serie, medido de las cuatro únicas formas en que se mide entrenar:
 * carga, repeticiones, tiempo y distancia. Un press de banca es carga y reps, una plancha
 * son segundos, correr son metros. La medida que cuenta para progresar la decide el
 * ejercicio a través de su {@code Metric}, no esta clase.
 */
public record Effort(int loadGrams, int reps, int seconds, int meters) implements ValueObject {

    public static final Effort NONE = new Effort(0, 0, 0, 0);

    /** 500 kg. No es un límite fisiológico: atrapa el 72,5 tecleado como 725. */
    public static final int MAX_LOAD_GRAMS = 500_000;

    private static final BigDecimal MAX_KILOGRAMS = BigDecimal.valueOf(MAX_LOAD_GRAMS, 3);
    private static final int GRAM_SCALE = 3;

    public Effort {
        NumberGuard.notNegative(loadGrams, "loadGrams");
        NumberGuard.notNegative(reps, "reps");
        NumberGuard.notNegative(seconds, "seconds");
        NumberGuard.notNegative(meters, "meters");
    }

    public static Result<Effort> create(int loadGrams, int reps, int seconds, int meters) {
        if (loadGrams < 0 || reps < 0 || seconds < 0 || meters < 0) {
            return Result.failure(TrainingErrors.MEASURES_MUST_NOT_BE_NEGATIVE);
        }

        if (loadGrams > MAX_LOAD_GRAMS) {
            return Result.failure(TrainingErrors.LOAD_OUT_OF_RANGE);
        }

        return Result.success(new Effort(loadGrams, reps, seconds, meters));
    }

    public static Result<Effort> ofKilograms(
        BigDecimal kilograms, int reps, int seconds, int meters) {

        // El límite se comprueba antes de escalar: un BigDecimal con exponente enorme
        // materializaría mil millones de dígitos en el setScale y nunca llegaría al guard.
        if (kilograms == null
            || kilograms.signum() < 0
            || kilograms.compareTo(MAX_KILOGRAMS) > 0) {

            return Result.failure(TrainingErrors.LOAD_OUT_OF_RANGE);
        }

        var grams = kilograms.setScale(GRAM_SCALE, RoundingMode.HALF_UP).unscaledValue();

        return create(grams.intValueExact(), reps, seconds, meters);
    }

    /** La carga de vuelta en kilos, como la espera quien lee la API. */
    public BigDecimal loadKilograms() {
        return BigDecimal.valueOf(loadGrams, GRAM_SCALE);
    }

    public boolean isZero() {
        return loadGrams == 0 && reps == 0 && seconds == 0 && meters == 0;
    }
}
