package atlas.application.presence.dto;

import java.util.List;

public record ProfileDto(
    String id,
    String displayName,
    String modelVersion,
    int templateCount,
    List<FaceTemplateDto> templates) {}
