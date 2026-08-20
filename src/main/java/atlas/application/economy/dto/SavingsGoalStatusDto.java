package atlas.application.economy.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SavingsGoalStatusDto(
    String id,
    String name,
    BigDecimal target,
    LocalDate deadline,
    BigDecimal monthlySaving,
    int monthsRemaining,
    BigDecimal projected,
    BigDecimal requiredMonthly,
    boolean reachable) {}
