# Druk Connect PostgreSQL migration

The backend now defaults to `jdbc:postgresql://localhost:5432/druk_connect`, username `root`, and the supplied local password. Override with `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`. Production already uses these three variables; set DB_URL to a PostgreSQL JDBC URL, not a MySQL URL or a postgres:// URI.

The PostgreSQL root login was created with no superuser privileges and owns druk_connect. MySQL was not modified. Restart any already-running backend to use the updated connection.

## Database contents

Migrated 25 tables and 811 rows, preserving primary keys, unique indexes, other indexes, 45 foreign keys (including their update/delete actions), 18 checks, defaults, nullability, and identity counters. All values were compared against MySQL with UTC dates and JSON normalized.

UUID columns stay CHAR(36) to match existing Java mappings. Boolean columns use BOOLEAN. MySQL DATETIME/TIMESTAMP columns use TIMESTAMP(6) WITH TIME ZONE; the source was read in UTC. Six token/hash String columns use VARCHAR to satisfy Hibernate validation.

## Starting the application

From the backend directory, run `./mvnw spring-boot:run` with JDK 21. The migrated local database already has a Flyway baseline at V1 and V2 applied. Normal startup uses Hibernate validate and Flyway migrations.

For a new empty PostgreSQL database, create the database and owner first. Startup automatically applies V1 (all tables/constraints/indexes) and V2 (character column alignment). These schema migrations contain no user data. Existing role initialization behavior is unchanged; schema-only creation does not populate the two application roles.

Do not run the old files in db/manual against PostgreSQL: those are historical MySQL scripts whose resulting schema is already included in V1.

## Export files

- postgresql-schema.sql: all tables, indexes, foreign keys, checks, and column alignment; intended for a new empty database.
- postgresql-data-migration.sql: the schema and all 811 source rows, loaded in one transaction with constraints validated and identity counters restored. Contains private account/session data; keep it private. Intended only for an empty database; do not rerun it against the migrated database.
- migration-row-counts.json: snapshot counts for every table.

For a manual import into another empty database: `psql -h localhost -p 5432 -U root -d druk_connect -v ON_ERROR_STOP=1 -f postgresql-data-migration.sql`. Before the first application startup against that manually imported database, use the one-time startup arguments `--spring.flyway.baseline-on-migrate=true --spring.flyway.baseline-version=1`. Subsequent starts need no baseline arguments. The local migrated database is already baselined.

## Behavior to review

PostgreSQL uses its default case-sensitive text comparisons; MySQL used case/accent-insensitive collation. Exact-case uniqueness and search behavior can consequently differ. PostgreSQL has no automatic MySQL ON UPDATE timestamp behavior; the existing Java entity lifecycle callbacks maintain application update timestamps.

MySQL remains a migration-time snapshot. Any writes made by an older running MySQL backend after the export are not synchronized into PostgreSQL.

Reference: https://docs.spring.io/spring-boot/how-to/data-initialization.html (Flyway initialization and PostgreSQL database module).

## Verification completed

All 53 Maven tests passed. The application started normally with Flyway and Hibernate validation against PostgreSQL; /v3/api-docs returned HTTP 200. The temporary verification server was stopped. All migrated row values were compared successfully against MySQL.
