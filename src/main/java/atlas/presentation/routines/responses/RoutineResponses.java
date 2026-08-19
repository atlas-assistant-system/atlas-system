package atlas.presentation.routines.responses;

import atlas.application.routines.dto.ComplianceStatsDto;
import atlas.application.routines.dto.HistoryDayDto;
import atlas.application.routines.dto.PeriodProgressDto;
import atlas.application.routines.dto.RoutineDto;
import atlas.application.routines.dto.RoutineSummaryDto;
import atlas.application.routines.dto.StreakDto;
import atlas.application.routines.dto.TodayRoutineDto;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RoutineResponses {

    private RoutineResponses() {}

    public static Map<String, Object> routine(RoutineDto dto) {
        var body = new LinkedHashMap<String, Object>();
        body.put("id", dto.id());
        body.put("name", dto.name());
        body.put("description", dto.description());
        body.put("target", dto.target().toPlainString());
        body.put("unit", dto.unit());
        body.put("period", dto.period());
        body.put("activeDays", dto.activeDays());
        body.put("daysOfMonth", dto.daysOfMonth());
        body.put("archived", dto.archived());

        return body;
    }

    public static Map<String, Object> summary(RoutineSummaryDto dto) {
        var body = new LinkedHashMap<String, Object>();
        body.put("id", dto.id());
        body.put("name", dto.name());
        body.put("target", dto.target().toPlainString());
        body.put("unit", dto.unit());
        body.put("period", dto.period());
        body.put("archived", dto.archived());

        return body;
    }

    public static List<Map<String, Object>> summaries(List<RoutineSummaryDto> summaries) {
        return summaries.stream().map(RoutineResponses::summary).toList();
    }

    public static Map<String, Object> progress(PeriodProgressDto dto) {
        var body = new LinkedHashMap<String, Object>();
        body.put("routineId", dto.routineId());
        body.put("periodStart", dto.periodStart().toString());
        body.put("periodEnd", dto.periodEnd().toString());
        body.put("logged", dto.logged().toPlainString());
        body.put("target", dto.target().toPlainString());
        body.put("unit", dto.unit());
        body.put("met", dto.met());
        body.put("closed", dto.closed());
        body.put("failed", dto.failed());

        return body;
    }

    public static List<Map<String, Object>> today(List<TodayRoutineDto> routines) {
        return routines.stream().map(today -> {
            var body = new LinkedHashMap<String, Object>();
            body.put("routine", summary(today.routine()));
            body.put("progress", progress(today.progress()));

            return (Map<String, Object>) body;
        }).toList();
    }

    public static List<Map<String, Object>> history(List<HistoryDayDto> days) {
        return days.stream().map(day -> {
            var body = new LinkedHashMap<String, Object>();
            body.put("day", day.day().toString());
            body.put("logged", day.logged().toPlainString());
            body.put("scheduled", day.scheduled());

            return (Map<String, Object>) body;
        }).toList();
    }

    public static Map<String, Object> streak(StreakDto dto) {
        var body = new LinkedHashMap<String, Object>();
        body.put("routineId", dto.routineId());
        body.put("current", dto.current());
        body.put("best", dto.best());

        return body;
    }

    public static List<Map<String, Object>> complianceStats(List<ComplianceStatsDto> stats) {
        return stats.stream().map(entry -> {
            var body = new LinkedHashMap<String, Object>();
            body.put("routineId", entry.routineId());
            body.put("name", entry.name());
            body.put("periodsClosed", entry.periodsClosed());
            body.put("periodsMet", entry.periodsMet());
            body.put("ratio", entry.ratio());

            return (Map<String, Object>) body;
        }).toList();
    }
}
