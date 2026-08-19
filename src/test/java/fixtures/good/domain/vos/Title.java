package fixtures.good.domain.vos;

import atlas.domain.sharedkernel.ddd.SingleValueObject;

public record Title(String value) implements SingleValueObject<String> {}
