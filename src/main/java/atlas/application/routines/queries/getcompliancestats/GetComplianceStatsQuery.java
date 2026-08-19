package atlas.application.routines.queries.getcompliancestats;

import atlas.application.routines.dto.ComplianceStatsDto;
import atlas.application.sharedkernel.cqrs.Query;
import atlas.domain.sharedkernel.results.Result;
import java.time.LocalDate;
import java.util.List;

public record GetComplianceStatsQuery(LocalDate from, LocalDate to, boolean includeArchived)
    implements Query<Result<List<ComplianceStatsDto>>> {}
