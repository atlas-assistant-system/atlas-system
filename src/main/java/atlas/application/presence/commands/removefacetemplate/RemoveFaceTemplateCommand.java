package atlas.application.presence.commands.removefacetemplate;

import atlas.domain.presence.BiometricProfileId;
import atlas.domain.presence.entities.FaceTemplateId;
import sharedkernel.application.cqrs.Command;
import sharedkernel.domain.results.Result;

public record RemoveFaceTemplateCommand(BiometricProfileId profileId, FaceTemplateId templateId)
    implements Command<Result<Void>> {}
