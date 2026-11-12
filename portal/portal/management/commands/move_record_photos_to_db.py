import mimetypes
from pathlib import Path

from django.conf import settings
from django.core.management.base import BaseCommand

from portal.models import ArchitectureRecord, BotanicalRecord, StoredFile


class Command(BaseCommand):
    help = ("Copy record photos from THIS computer's media/ folder into the shared database, so "
            "everyone sees them. Each person who uploaded record photos runs it once on their own "
            "computer. Photos already in the database are skipped, so it is safe to run again.")

    def add_arguments(self, parser):
        parser.add_argument("--dry-run", action="store_true", help="report what would be copied, copy nothing")

    def handle(self, *args, **opts):
        say = self.stdout.write
        media = Path(settings.MEDIA_ROOT)
        copied = present = missing = 0
        for model in (BotanicalRecord, ArchitectureRecord):
            for record in model._base_manager.exclude(image=""):   # deleted records too: they can be restored
                name = record.image.name
                if StoredFile.objects.filter(name=name).exists():
                    present += 1
                    continue
                local = media / name
                if not local.is_file():
                    missing += 1
                    say(f"  not on this computer: {record} ({name})")
                    continue
                if not opts["dry_run"]:
                    data = local.read_bytes()
                    StoredFile.objects.create(name=name, content=data, size=len(data),
                                              content_type=mimetypes.guess_type(name)[0] or "application/octet-stream")
                copied += 1
                say(f"  {'would copy' if opts['dry_run'] else 'copied'}: {record} ({name})")
        say(f"{copied} {'to copy' if opts['dry_run'] else 'copied'}, {present} already in the database, "
            f"{missing} not on this computer.")
        if missing:
            say("Photos not on this computer must be copied by whoever uploaded them, or re-uploaded "
                "by editing the record.")
