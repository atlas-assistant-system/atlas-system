package atlas.application.presence.commands.removefacetemplate;

import atlas.application.sharedkernel.cqrs.Command;
import atlas.domain.presence.BiometricProfileId;
import atlas.domain.presence.entities.FaceTemplateId;
import atlas.domain.sharedkernel.results.Result;

public record RemoveFaceTemplateCommand(BiometricProfileId profileId, FaceTemplateId templateId)
    implements Command<Result<Void>> {}
