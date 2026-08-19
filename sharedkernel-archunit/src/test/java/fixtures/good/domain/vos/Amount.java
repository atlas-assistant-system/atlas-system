package fixtures.good.domain.vos;

import sharedkernel.domain.ddd.ValueObject;

public record Amount(long value, String currency) implements ValueObject {}
