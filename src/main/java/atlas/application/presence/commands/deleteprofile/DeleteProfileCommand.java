package atlas.application.presence.commands.deleteprofile;

import atlas.domain.presence.BiometricProfileId;
import sharedkernel.application.cqrs.Command;
import sharedkernel.domain.results.Result;

public record DeleteProfileCommand(BiometricProfileId profileId) implements Command<Result<Void>> {}
