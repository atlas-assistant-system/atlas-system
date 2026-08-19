package atlas.application.routines.dto;

public record ComplianceStatsDto(String routineId, String name, int periodsClosed, int periodsMet, double ratio) {}
