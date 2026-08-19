package atlas.application.presence.commands.deleteprofile;

import atlas.application.sharedkernel.cqrs.Command;
import atlas.domain.presence.BiometricProfileId;
import atlas.domain.sharedkernel.results.Result;

public record DeleteProfileCommand(BiometricProfileId profileId) implements Command<Result<Void>> {}
