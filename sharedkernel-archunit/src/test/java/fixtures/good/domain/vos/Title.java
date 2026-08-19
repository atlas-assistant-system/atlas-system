package fixtures.good.domain.vos;

import sharedkernel.domain.ddd.SingleValueObject;

public record Title(String value) implements SingleValueObject<String> {}
