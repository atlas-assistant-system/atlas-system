package fixtures.good.domain.vos;

import atlas.domain.sharedkernel.ddd.ValueObject;

public record Amount(long value, String currency) implements ValueObject {}
