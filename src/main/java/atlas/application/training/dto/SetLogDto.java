package atlas.application.training.dto;

public record SetLogDto(
    String id, String exerciseId, int position, EffortDto planned, EffortDto actual) {}
