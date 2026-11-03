import random

from django.core.management.base import BaseCommand

from portal.models import (ArchitectureRecord, BotanicalRecord, Collectible, CollectibleCategory,
                           CollectibleUnlock, StoredFile)

from .seed_records import file_name, placeholder

VISITORS = ["visitor01", "visitor02", "visitor03", "visitor04", "visitor05"]


class Command(BaseCommand):
    help = ("Create SAMPLE collectibles, one for each Published botanical / architecture record, "
            "plus a few visitor unlocks. Run seed_records first. Safe to run twice. "
            "Use --clear to remove the sample collectibles.")

    def add_arguments(self, parser):
        parser.add_argument("--clear", action="store_true",
                            help="delete the sample collectibles (and their unlocks and pictures), then stop")

    def handle(self, *args, **opts):
        say = self.stdout.write if opts["verbosity"] > 0 else (lambda *_: None)

        if opts["clear"]:
            sample = Collectible.objects.filter(image__contains="sample-")
            names = list(sample.values_list("image", flat=True))
            CollectibleUnlock.objects.filter(collectible__in=sample).delete()
            deleted, _ = sample.delete()
            StoredFile.objects.filter(name__in=names).delete()
            say(f"removed {deleted} sample collectibles")
            return

        flora = CollectibleCategory.objects.filter(name="Flora").first()
        architecture = CollectibleCategory.objects.filter(name="Architecture").first()
        if flora is None or architecture is None:
            self.stderr.write("The default categories are missing. Run `python manage.py migrate` first.")
            return

        rng = random.Random(2026)        # same "random" data every time
        rarity = lambda: rng.choices(
            [Collectible.COMMON, Collectible.UNCOMMON, Collectible.RARE, Collectible.HIDDEN],
            weights=[4, 3, 2, 1])[0]

        made = []
        for record in BotanicalRecord.objects.filter(status="PUBLISHED", collectible__isnull=True):
            made.append(self._make(record.common_name, record.identification_features.split("\n")[0],
                                   flora, rarity(), rng, botanical_record=record))
        for record in ArchitectureRecord.objects.filter(status="PUBLISHED", collectible__isnull=True):
            made.append(self._make(record.name, record.description, architecture, rarity(), rng,
                                   architecture_record=record))
        say(f"collectibles created: {len(made)}")
        if not made and not Collectible.objects.exists():
            say("No Published records found. Run `python manage.py seed_records` first.")

        # a few unlocks, so some collectibles are "in use" and cannot be deleted
        added = 0
        for collectible in made[:6]:
            for visitor in rng.sample(VISITORS, rng.randint(1, 3)):
                _, created = CollectibleUnlock.objects.get_or_create(
                    collectible=collectible, visitor_username=visitor)
                added += created
        say(f"visitor unlocks created: {added}")

    @staticmethod
    def _make(name, description, category, rarity, rng, **link):
        collectible = Collectible(name=name, description=f"SAMPLE: {description}"[:500],
                                  category=category, rarity=rarity, status=Collectible.PUBLISHED, **link)
        collectible.image.save(file_name(name), placeholder(name, rng), save=False)
        collectible.save()
        return collectible
