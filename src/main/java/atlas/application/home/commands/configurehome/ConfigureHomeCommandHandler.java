package atlas.application.home.commands.configurehome;

import atlas.application.home.dto.HomeProfileDto;
import atlas.application.home.mappers.HomeMapper;
import atlas.application.home.ports.HomeUnitOfWork;
import atlas.application.sharedkernel.cqrs.CommandHandler;
import atlas.domain.home.HomeProfile;
import atlas.domain.home.vos.HomeLocation;
import atlas.domain.sharedkernel.results.Result;
import java.time.Clock;

public final class ConfigureHomeCommandHandler
    implements CommandHandler<ConfigureHomeCommand, Result<HomeProfileDto>> {

    private final HomeUnitOfWork unitOfWork;
    private final Clock clock;

    public ConfigureHomeCommandHandler(HomeUnitOfWork unitOfWork, Clock clock) {
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    @Override
    public Result<HomeProfileDto> handle(ConfigureHomeCommand command) {
        var location = HomeLocation.create(
            command.locationName(), command.latitude(), command.longitude(), command.timeZone());
        if (location.isFailure()) {
            return Result.failure(location.error());
        }

        return unitOfWork.execute(() -> {
            var profiles = unitOfWork.profiles();
            var existing = profiles.get(command.profileId());
            if (existing.isPresent()) {
                var profile = existing.get();
                var changed = profile.reconfigure(location.value(), command.newsCategories(), clock.instant());
                if (changed.isFailure()) {
                    return Result.<HomeProfileDto>failure(changed.error());
                }
                profiles.update(profile);

                return Result.success(HomeMapper.toDto(profile));
            }

            var configured = HomeProfile.configure(
                command.profileId(), location.value(), command.newsCategories(), clock.instant());
            if (configured.isFailure()) {
                return Result.<HomeProfileDto>failure(configured.error());
            }
            profiles.create(configured.value());

            return Result.success(HomeMapper.toDto(configured.value()));
        });
    }
}
