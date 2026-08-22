package atlas.domain.training;

import atlas.domain.sharedkernel.results.Error;

public final class TrainingErrors {

    public static final Error MEASURES_MUST_NOT_BE_NEGATIVE = Error.validation(
        "Training.MeasuresMustNotBeNegative",
        "A set is measured in grams, reps, seconds and metres, never below zero.");

    public static final Error LOAD_OUT_OF_RANGE =
        Error.validation("Training.LoadOutOfRange", "That is not a load anybody lifts.");

    private TrainingErrors() {}
}
