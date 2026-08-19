package atlas.application.appointments.dto;

import atlas.application.sharedkernel.paging.Page;
import java.time.LocalDate;

public record AppointmentPageDto(
    Page<AppointmentSummaryDto> page, LocalDate anchor, LocalDate previousAnchor, LocalDate nextAnchor) {}
