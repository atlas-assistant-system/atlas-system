package atlas.application.economy.dto;

import java.math.BigDecimal;

public record BudgetDto(String id, String category, String label, String icon, BigDecimal limit) {}
