package atlas.application.economy.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SavingsGoalDto(String id, String name, BigDecimal target, LocalDate deadline) {}
