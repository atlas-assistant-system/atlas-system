package atlas.application.routines.queries.getcompliancestats;

import atlas.application.routines.dto.ComplianceStatsDto;
import java.time.LocalDate;
import java.util.List;
import sharedkernel.application.cqrs.Query;
import sharedkernel.domain.results.Result;

public record GetComplianceStatsQuery(LocalDate from, LocalDate to, boolean includeArchived)
    implements Query<Result<List<ComplianceStatsDto>>> {}
