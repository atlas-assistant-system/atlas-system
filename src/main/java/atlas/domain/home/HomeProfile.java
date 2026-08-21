package atlas.domain.home;

import atlas.domain.home.enums.NewsCategory;
import atlas.domain.home.events.HomeConfiguredEvent;
import atlas.domain.home.vos.HomeLocation;
import atlas.domain.sharedkernel.ddd.AggregateRoot;
import atlas.domain.sharedkernel.exceptions.GuardException;
import atlas.domain.sharedkernel.guards.ObjectGuard;
import atlas.domain.sharedkernel.results.Result;
import java.time.Instant;
import java.util.Set;

public final class HomeProfile extends AggregateRoot<HomeProfileId> {

    private HomeLocation location;
    private Set<NewsCategory> newsCategories;

    private HomeProfile(HomeProfileId id, HomeLocation location, Set<NewsCategory> newsCategories) {
        super(ObjectGuard.notNull(id, "id"));
        this.location = ObjectGuard.notNull(location, "location");
        this.newsCategories = Set.copyOf(ObjectGuard.notNull(newsCategories, "newsCategories"));
        if (this.newsCategories.isEmpty()) {
            throw GuardException.forParameter("newsCategories", "cannot be empty");
        }
    }

    public static Result<HomeProfile> configure(
        HomeProfileId id,
        HomeLocation location,
        Set<NewsCategory> newsCategories,
        Instant occurredOn) {

        if (newsCategories == null || newsCategories.isEmpty()) {
            return Result.failure(HomeErrors.NEWS_CATEGORIES_REQUIRED);
        }

        var profile = new HomeProfile(id, location, newsCategories);
        profile.registerEvent(new HomeConfiguredEvent(id, occurredOn));

        return Result.success(profile);
    }

    public static HomeProfile rehydrate(
        HomeProfileId id,
        HomeLocation location,
        Set<NewsCategory> newsCategories) {

        return new HomeProfile(id, location, newsCategories);
    }

    public Result<Void> reconfigure(
        HomeLocation newLocation,
        Set<NewsCategory> newNewsCategories,
        Instant occurredOn) {

        if (newNewsCategories == null || newNewsCategories.isEmpty()) {
            return Result.failure(HomeErrors.NEWS_CATEGORIES_REQUIRED);
        }

        location = ObjectGuard.notNull(newLocation, "newLocation");
        newsCategories = Set.copyOf(newNewsCategories);
        registerEvent(new HomeConfiguredEvent(id(), occurredOn));

        return Result.success();
    }

    public HomeLocation location() {
        return location;
    }

    public Set<NewsCategory> newsCategories() {
        return newsCategories;
    }
}
