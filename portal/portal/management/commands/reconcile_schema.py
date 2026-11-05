from django.apps import apps
from django.core.management.base import BaseCommand, CommandError
from django.db import connection, transaction
from django.db.migrations.loader import MigrationLoader

APP = "portal"


class Command(BaseCommand):
    help = ("Compare the database with the portal's code and list what the code doesn't know about: "
            "extra columns and tables, and applied migrations whose files don't exist (e.g. from a "
            "teammate's unpushed branch). Changes nothing unless --apply is given; then it DROPS the "
            "extra columns (their data is lost) and forgets the unknown migrations. Extra tables are "
            "only reported. SQL Server (Azure) only for --apply.")

    def add_arguments(self, parser):
        parser.add_argument("--apply", action="store_true",
                            help="drop the extra columns and remove the unknown migration records")

    def handle(self, *args, **opts):
        say = self.stdout.write
        with connection.cursor() as cursor:
            extra_columns = self._extra_columns(cursor)
            extra_tables = self._extra_tables(cursor)
            unknown_migrations = self._unknown_migrations(cursor)

        say(f"Database: {connection.vendor} {connection.settings_dict['NAME']}")
        say("Extra columns (in the database, not in the code):")
        for table, column in extra_columns:
            say(f"  {table}.{column}")
        say("  none" if not extra_columns else "")
        say("Extra tables (reported only, never dropped):")
        for table in extra_tables:
            say(f"  {table}")
        say("  none" if not extra_tables else "")
        say("Applied migrations with no migration file:")
        for name in unknown_migrations:
            say(f"  {APP}.{name}")
        say("  none" if not unknown_migrations else "")

        if not opts["apply"]:
            if extra_columns or unknown_migrations:
                say("Nothing changed. Run again with --apply to remove the columns and migration records above.")
            return
        if connection.vendor != "microsoft":
            raise CommandError("--apply only supports SQL Server (Azure).")

        with transaction.atomic(), connection.cursor() as cursor:
            for table, column in extra_columns:
                self._drop_column(cursor, table, column)
                say(f"dropped column {table}.{column}")
            for name in unknown_migrations:
                cursor.execute("DELETE FROM django_migrations WHERE app = %s AND name = %s", [APP, name])
                say(f"forgot migration {APP}.{name}")
        say("Done.")

    # --- comparing --------------------------------------------------------
    @staticmethod
    def _model_tables():
        """{table: set of column names} for every portal model, including many-to-many tables."""
        tables = {}
        for model in apps.get_app_config(APP).get_models():
            tables[model._meta.db_table] = {f.column for f in model._meta.local_concrete_fields}
            for m2m in model._meta.local_many_to_many:
                through = m2m.remote_field.through._meta
                tables[through.db_table] = {f.column for f in through.local_concrete_fields}
        return tables

    def _extra_columns(self, cursor):
        existing = set(connection.introspection.table_names(cursor))
        extra = []
        for table, expected in sorted(self._model_tables().items()):
            if table not in existing:
                continue
            actual = [c.name for c in connection.introspection.get_table_description(cursor, table)]
            extra += [(table, c) for c in actual if c not in expected]
        return extra

    def _extra_tables(self, cursor):
        known = set(self._model_tables())
        return sorted(t for t in connection.introspection.table_names(cursor)
                      if t.startswith(f"{APP}_") and t not in known)

    @staticmethod
    def _unknown_migrations(cursor):
        on_disk = {name for app, name in MigrationLoader(None, ignore_no_migrations=True).disk_migrations
                   if app == APP}
        if "django_migrations" not in connection.introspection.table_names(cursor):
            return []
        cursor.execute("SELECT name FROM django_migrations WHERE app = %s", [APP])
        return sorted(name for (name,) in cursor.fetchall() if name not in on_disk)

    # --- dropping (SQL Server) -------------------------------------------
    @staticmethod
    def _drop_column(cursor, table, column):
        qn = connection.ops.quote_name
        # SQL Server won't drop a column while a default, check, index or key still uses it.
        cursor.execute(
            "SELECT dc.name FROM sys.default_constraints dc "
            "JOIN sys.columns c ON c.object_id = dc.parent_object_id AND c.column_id = dc.parent_column_id "
            "WHERE dc.parent_object_id = OBJECT_ID(%s) AND c.name = %s", [table, column])
        for (name,) in cursor.fetchall():
            cursor.execute(f"ALTER TABLE {qn(table)} DROP CONSTRAINT {qn(name)}")
        cursor.execute("SELECT name FROM sys.indexes WHERE object_id = OBJECT_ID(%s)", [table])
        plain_indexes = {name for (name,) in cursor.fetchall()}   # incl. unique indexes
        for name, info in connection.introspection.get_constraints(cursor, table).items():
            if column not in (info["columns"] or []):
                continue
            if info["primary_key"]:
                raise CommandError(f"{table}.{column} is part of the primary key; not dropping it.")
            # a UNIQUE / PRIMARY KEY *constraint* is dropped as a constraint, a CREATE INDEX as an index
            cursor.execute("SELECT 1 FROM sys.objects WHERE name = %s AND parent_object_id = OBJECT_ID(%s)",
                           [name, table])
            if cursor.fetchone():
                cursor.execute(f"ALTER TABLE {qn(table)} DROP CONSTRAINT {qn(name)}")
            elif name in plain_indexes:
                cursor.execute(f"DROP INDEX {qn(name)} ON {qn(table)}")
        cursor.execute(f"ALTER TABLE {qn(table)} DROP COLUMN {qn(column)}")
