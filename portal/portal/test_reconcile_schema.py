"""Tests for `manage.py reconcile_schema` (finding database changes the code doesn't know about)."""
import io

from django.core.management import call_command
from django.core.management.base import CommandError
from django.db import connection
from django.test import TransactionTestCase


class ReconcileSchemaTests(TransactionTestCase):
    def run_command(self, *args):
        out = io.StringIO()
        call_command("reconcile_schema", *args, stdout=out)
        return out.getvalue()

    def test_clean_database_reports_nothing(self):
        out = self.run_command()
        self.assertEqual(out.count("  none"), 3)

    def test_reports_unpushed_column_and_migration_without_changing_them(self):
        with connection.cursor() as cursor:
            cursor.execute("ALTER TABLE portal_collectible ADD COLUMN safety_note text NOT NULL DEFAULT ''")
            cursor.execute("INSERT INTO django_migrations (app, name, applied) "
                           "VALUES ('portal', '0007_teammate_safety_note', CURRENT_TIMESTAMP)")
        try:
            out = self.run_command()
            self.assertIn("portal_collectible.safety_note", out)
            self.assertIn("portal.0007_teammate_safety_note", out)
            self.assertIn("Nothing changed", out)
            with connection.cursor() as cursor:
                columns = [c.name for c in connection.introspection.get_table_description(cursor, "portal_collectible")]
            self.assertIn("safety_note", columns)
        finally:
            with connection.cursor() as cursor:
                cursor.execute("ALTER TABLE portal_collectible DROP COLUMN safety_note")
                cursor.execute("DELETE FROM django_migrations WHERE name = '0007_teammate_safety_note'")

    def test_apply_refuses_on_sqlite(self):
        with self.assertRaises(CommandError):
            self.run_command("--apply")
