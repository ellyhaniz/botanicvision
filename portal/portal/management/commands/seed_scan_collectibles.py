import io
import random
from pathlib import Path

from django.core.files.base import ContentFile
from django.core.management.base import BaseCommand
from PIL import Image, ImageOps

from portal.classifier import CLASS_INFO
from portal.models import ArchitectureRecord, BotanicalRecord, Collectible, CollectibleCategory

from .seed_records import placeholder

# Checked against NParks, PublicArtSG and the National Heritage Board (Nov 2026).
# fix_card_facts uses these too, to correct cards already in the database.
VANDA_FACT = ("Singapore's national flower, Vanda Miss Joaquim, is a Vanda hybrid bred by Agnes "
              "Joaquim in 1893. It was chosen in 1981, making Singapore the only country whose "
              "national flower is a hybrid.")
SWAN_LAKE_FACT = ("The lake is named after a pair of mute swans brought over from Amsterdam. The bronze "
                  "sculpture in the middle, Flight of Swans, was added in 2006.")
SWING_ME_MAMA_DESCRIPTION = ("A stone sculpture of a mother swinging her child, by Zimbabwean artist "
                             "Dominic Benhura.")
SWING_ME_MAMA_FACT = ("A gift from the Rotary Clubs of Singapore, it stood in Fort Canning Park before "
                      "moving to the Gardens. Benhura later gave a work of the same name to Nelson Mandela.")
# The card shows "<style>, <built>", e.g. "Gateway, main entrance since 1859".
TANGLIN_GATE_STYLE, TANGLIN_GATE_BUILT = "Gateway", "main entrance since 1859"

# One collectible per class of the PlantDex image model (portal/ml/labels.txt).
PLANTS = {
    "allamanda_cathartica": ("Apocynaceae", Collectible.COMMON,
        "Large yellow trumpet-shaped flowers with five overlapping petals.\n"
        "Glossy leaves in whorls.\nVigorous climbing shrub.",
        "All parts of the plant are poisonous if eaten, and the milky sap can irritate skin."),
    "calathea": ("Marantaceae", Collectible.UNCOMMON,
        "Boldly patterned leaves with stripes or feathering.\nLeaves fold upward at night.",
        "Its leaves rise and fall every day, which is why its family is known as prayer plants."),
    "cheilocostus_speciosus": ("Costaceae", Collectible.RARE,
        "White crinkled flowers that look like crepe paper.\nRed cone-shaped flower head.\n"
        "Leaves arranged in a spiral up the stem.",
        "The leaves climb the stem in a spiral, which gives the family its name: spiral gingers."),
    "costus": ("Costaceae", Collectible.UNCOMMON,
        "Waxy red cone-shaped flower head.\nLeaves spiralling around a tall stem.",
        "Despite the name spiral ginger, it is not a true ginger: it has its own family, Costaceae."),
    "crinum_asiaticum": ("Amaryllidaceae", Collectible.UNCOMMON,
        "Clusters of white flowers with narrow, separate petals on a tall stalk.\n"
        "Large strap-shaped leaves growing from a big bulb.",
        "It is also called the poison bulb, because the bulb is toxic."),
    "dendrobium_hybrid": ("Orchidaceae", Collectible.COMMON,
        "Sprays of flowers along cane-like stems.\nOften white petals fading into magenta tips.",
        "Dendrobium is one of the largest orchid genera, with more than a thousand species."),
    "hymenocallis": ("Amaryllidaceae", Collectible.UNCOMMON,
        "White flowers with long, thin, curling petals.\n"
        "A thin white cup joins the stamens at the centre.",
        "The cup in the middle is a membrane joining the stamens, which tells it apart from crinum lilies."),
    "oncidium_hybrid": ("Orchidaceae", Collectible.COMMON,
        "Masses of small yellow flowers with red-brown markings.\nLong arching sprays.",
        "Each flower's wide lip looks like a dancer's skirt, which gives it its name."),
    "spathiphyllum": ("Araceae", Collectible.COMMON,
        "A white hood-like spathe around a cream spike.\nGlossy dark green leaves.",
        "The white 'petal' is not a petal at all: it is a modified leaf called a spathe."),
    "vanda_hybrid": ("Orchidaceae", Collectible.RARE,
        "Flat, rounded flowers, often with chequered patterning.\nThick strap-shaped leaves in two rows.",
        VANDA_FACT),
}
STRUCTURES = {
    "swan_lake": ("Tanglin Core", "Ornamental lake", "1866", Collectible.COMMON,
        "The oldest ornamental lake in the Gardens, with a bronze sculpture of swans in flight.",
        SWAN_LAKE_FACT),
    "swing_me_mama": ("Near Swan Lake", "Sculpture", "1999", Collectible.RARE,
        SWING_ME_MAMA_DESCRIPTION, SWING_ME_MAMA_FACT),
    "tanglin_gate": ("Junction of Holland Road and Napier Road", TANGLIN_GATE_STYLE, TANGLIN_GATE_BUILT,
        Collectible.COMMON,
        "The Gardens' main entrance: white pillars and cut-metal gates patterned with leaves.",
        "The Gardens became Singapore's first UNESCO World Heritage Site in 2015."),
}


class Command(BaseCommand):
    help = ("Create one collectible for each class the PlantDex model recognises, with its scan "
            "label set, so scanning unlocks it. Reuses an existing Published record or collectible "
            "with the same name. Safe to run twice.")

    def add_arguments(self, parser):
        parser.add_argument("--photos", help="dataset folder with one sub-folder of photos per class "
                                             "(e.g. ...\\UOW\\FYP\\dataset); its first photo becomes the "
                                             "card picture. Without it, a placeholder picture is used.")

    def handle(self, *args, **opts):
        say = self.stdout.write
        flora = CollectibleCategory.objects.filter(name="Flora").first()
        architecture = CollectibleCategory.objects.filter(name="Architecture").first()
        if flora is None or architecture is None:
            self.stderr.write("The default categories are missing. Run `python manage.py migrate` first.")
            return
        photos = Path(opts["photos"]) if opts["photos"] else None
        rng = random.Random(2026)

        for label in list(PLANTS) + list(STRUCTURES):
            name = CLASS_INFO[label]["common"]
            if Collectible.objects.filter(model_label=label).exists():
                say(f"  {label:24} already has a collectible")
                continue

            if label in PLANTS:
                family, rarity, features, fact = PLANTS[label]
                record = BotanicalRecord.objects.filter(common_name__iexact=name).first()
                if record is None:
                    record = BotanicalRecord(common_name=name, scientific_name=CLASS_INFO[label]["scientific"],
                                             family=family, identification_features=features, fun_fact=fact)
                    record.image.save(*self._picture(photos, label, name, rng), save=False)
                    record.save()
                description, category, link = features.split("\n")[0], flora, {"botanical_record": record}
            else:
                location, style, built, rarity, description, fact = STRUCTURES[label]
                record = ArchitectureRecord.objects.filter(name__iexact=name).first()
                if record is None:
                    record = ArchitectureRecord(name=name, location=location, architectural_style=style,
                                                construction_date=built, description=description, fun_fact=fact)
                    record.image.save(*self._picture(photos, label, name, rng), save=False)
                    record.save()
                category, link = architecture, {"architecture_record": record}

            existing = getattr(record, "collectible", None) if record.pk else None
            if existing is not None:
                existing.model_label = label
                existing.save(update_fields=["model_label", "updated_at"])
                say(f"  {label:24} linked to the existing collectible '{existing.name}'")
                continue
            card = Collectible(name=name, description=description, category=category, rarity=rarity,
                               status=Collectible.PUBLISHED, model_label=label, **link)
            card.image.save(*self._picture(photos, label, name, rng), save=False)
            card.save()
            say(f"  {label:24} created '{name}'")

    @staticmethod
    def _picture(photos, label, name, rng):
        """(file name, content): a photo of the class shrunk to 800 px, or a generated placeholder.
        Names start with "scan-", not "sample-", so `seed_collectibles --clear` leaves them alone."""
        files = sorted((photos / label).glob("*.[jJ][pP][gG]")) if photos else []
        if not files:
            return f"scan-{label}.png", placeholder(name, rng)
        image = ImageOps.exif_transpose(Image.open(files[len(files) // 2])).convert("RGB")
        image.thumbnail((800, 800))
        out = io.BytesIO()
        image.save(out, "JPEG", quality=85)
        return f"scan-{label}.jpg", ContentFile(out.getvalue())
