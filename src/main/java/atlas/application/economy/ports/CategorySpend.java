package atlas.application.economy.ports;

import atlas.domain.economy.enums.Category;

public record CategorySpend(Category category, long cents) {}
