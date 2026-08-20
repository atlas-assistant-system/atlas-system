package atlas.application.economy.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BalanceDto(
    LocalDate from,
    LocalDate to,
    BigDecimal income,
    BigDecimal expense,
    BigDecimal net) {}
