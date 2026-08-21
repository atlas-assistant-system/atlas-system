package atlas.infrastructure.home.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import atlas.application.sharedkernel.events.ImmediateEventDelivery;
import atlas.application.sharedkernel.events.PendingEventDispatcher;
import atlas.application.sharedkernel.events.SimpleDomainEventPublisher;
import atlas.domain.home.HomeProfile;
import atlas.domain.home.HomeProfileId;
import atlas.domain.home.enums.NewsCategory;
import atlas.domain.home.vos.HomeLocation;
import atlas.infrastructure.sharedkernel.persistence.Migrations;
import atlas.infrastructure.sharedkernel.persistence.SchemaMigrator;
import java.sql.DriverManager;
import java.time.Clock;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SqliteHomeUnitOfWorkIT {

    @Test
    void shouldPersistAndReloadAHomeProfile() throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            connection.setAutoCommit(false);
            new SchemaMigrator(connection, Clock.systemUTC()).migrate(Migrations.load(
                SqliteHomeUnitOfWorkIT.class,
                "/db-migrations/home",
                "V001__create_home_profiles.sql"));
            var unitOfWork = new SqliteHomeUnitOfWork(
                connection,
                new ImmediateEventDelivery(new PendingEventDispatcher(new SimpleDomainEventPublisher())));
            var id = HomeProfileId.of("P00000001");
            var location = HomeLocation.create("Las Palmas", 28.1235, -15.4363, "Atlantic/Canary").value();
            var profile = HomeProfile.configure(id, location, Set.of(NewsCategory.AI), Clock.systemUTC().instant())
                .value();

            unitOfWork.run(() -> unitOfWork.profiles().create(profile));

            var reloaded = unitOfWork.profiles().get(id);
            assertThat(reloaded).isPresent();
            assertThat(reloaded.orElseThrow().location()).isEqualTo(location);
            assertThat(reloaded.orElseThrow().newsCategories()).containsExactly(NewsCategory.AI);
        }
    }
}
