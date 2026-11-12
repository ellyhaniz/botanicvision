"""
Record photos are stored in the shared database (StoredFile), not in one computer's media/
folder, so every team member's portal can show them. move_record_photos_to_db copies photos
uploaded before this change from the local media/ folder into the database.
"""
import shutil
import tempfile
from pathlib import Path
from unittest import mock

from django.core.files.base import ContentFile
from django.core.management import call_command
from django.test import TestCase, override_settings

from .models import ArchitectureRecord, BotanicalRecord, StoredFile

GIF = (b"GIF89a\x01\x00\x01\x00\x80\x00\x00\x00\x00\x00\xff\xff\xff!\xf9\x04\x01\x00\x00\x00\x00"
       b",\x00\x00\x00\x00\x01\x00\x01\x00\x00\x02\x02D\x01\x00;")


def botanical(name="Tembusu", image=None):
    record = BotanicalRecord(common_name=name, scientific_name="Cyrtophyllum fragrans", family="Gentianaceae",
                             identification_features="Tall tree", fun_fact="On the $5 note")
    if image is None:
        record.image.save("tembusu.gif", ContentFile(GIF), save=False)
    else:
        record.image.name = image          # a file name only, as saved before photos moved to the database
    record.save()
    return record


class RecordPhotoStorageTests(TestCase):
    def test_new_record_photo_is_saved_in_the_database(self):
        record = botanical()
        stored = StoredFile.objects.get(name=record.image.name)
        self.assertTrue(record.image.name.startswith("botanical/"))
        self.assertEqual(bytes(stored.content), GIF)
        self.assertIn("/files/", record.image.url)     # served from the database, not /media/

    def test_architecture_photo_is_saved_in_the_database(self):
        record = ArchitectureRecord(name="Tanglin Gate", location="Holland Road", architectural_style="Gateway",
                                    construction_date="1859", description="Main entrance")
        record.image.save("gate.gif", ContentFile(GIF))
        self.assertTrue(StoredFile.objects.filter(name=record.image.name).exists())


class MoveRecordPhotosTests(TestCase):
    def setUp(self):
        self.media = Path(tempfile.mkdtemp())
        self.addCleanup(shutil.rmtree, self.media, ignore_errors=True)

    def run_command(self, *args):
        out = mock.Mock()
        with override_settings(MEDIA_ROOT=self.media):
            call_command("move_record_photos_to_db", *args, stdout=out)
        return " ".join(str(c.args[0]) for c in out.write.call_args_list)

    def local_photo(self, name):
        path = self.media / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(GIF)

    def test_copies_local_photos_once_and_reports_missing_ones(self):
        self.local_photo("botanical/2026/10/old.gif")
        here = botanical("Here", image="botanical/2026/10/old.gif")
        botanical("Elsewhere", image="botanical/2026/10/someone-else.gif")

        report = self.run_command()
        self.assertEqual(bytes(StoredFile.objects.get(name=here.image.name).content), GIF)
        self.assertIn("1 copied", report)
        self.assertIn("1 not on this computer", report)

        again = self.run_command()                                  # safe to run twice
        self.assertIn("0 copied, 1 already in the database", again)
        self.assertEqual(StoredFile.objects.filter(name=here.image.name).count(), 1)

    def test_dry_run_copies_nothing(self):
        self.local_photo("botanical/2026/10/old.gif")
        botanical("Here", image="botanical/2026/10/old.gif")
        self.assertIn("1 to copy", self.run_command("--dry-run"))
        self.assertFalse(StoredFile.objects.exists())

    def test_deleted_records_are_included(self):
        self.local_photo("botanical/2026/10/gone.gif")
        record = botanical("Gone", image="botanical/2026/10/gone.gif")
        record.soft_delete()
        self.run_command()
        self.assertTrue(StoredFile.objects.filter(name="botanical/2026/10/gone.gif").exists())
