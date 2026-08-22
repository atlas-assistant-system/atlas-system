package atlas.infrastructure.sharedkernel.persistence;

record AppliedMigration(int version, String name, String checksum) {}
