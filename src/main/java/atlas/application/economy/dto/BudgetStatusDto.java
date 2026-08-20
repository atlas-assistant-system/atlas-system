package atlas.application.economy.dto;

import java.math.BigDecimal;

public record BudgetStatusDto(
    String id,
    String category,
    String label,
    String icon,
    BigDecimal limit,
    BigDecimal spent,
    BigDecimal projected,
    String status) {}
