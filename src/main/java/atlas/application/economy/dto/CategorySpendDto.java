package atlas.application.economy.dto;

import java.math.BigDecimal;

public record CategorySpendDto(
    String category,
    String label,
    String icon,
    BigDecimal total,
    BigDecimal percentage) {}
