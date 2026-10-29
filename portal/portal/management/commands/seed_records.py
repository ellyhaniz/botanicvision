import io
import random

from django.core.files.base import ContentFile
from django.core.management.base import BaseCommand
from PIL import Image, ImageDraw

from portal.models import ArchitectureRecord, BotanicalRecord

# ---------------------------------------------------------------------------
# SAMPLE DATA for development only.
# Plant names, scientific names and families are real. Everything else (status,
# styles, dates, locations) is picked at random, so do NOT treat it as fact.
# ---------------------------------------------------------------------------
PLANTS = [
    ("Tembusu", "Cyrtophyllum fragrans", "Gentianaceae",
     "Dark, deeply fissured bark.\nSmall cream fragrant flowers.\nWide, spreading crown.",
     "Appears on the Singapore five-dollar note."),
    ("Rain Tree", "Samanea saman", "Fabaceae",
     "Very broad, umbrella-shaped crown.\nFeathery leaves that fold at night.\nPink powder-puff flowers.",
     "The leaflets fold up at night and in wet weather."),
    ("Vanda Miss Joaquim", "Papilionanthe Miss Joaquim", "Orchidaceae",
     "Slender climbing stems with pencil-like leaves.\nPink-mauve flowers.",
     "Singapore's national flower."),
    ("Frangipani", "Plumeria rubra", "Apocynaceae",
     "Thick, fleshy branches with white sap.\nFragrant five-petalled flowers in clusters at the branch tips.",
     "The scent is strongest in the evening."),
    ("Traveller's Palm", "Ravenala madagascariensis", "Strelitziaceae",
     "Giant fan of paddle-shaped leaves in a single flat plane.\nNot a true palm.",
     "SAMPLE: replace with a verified fun fact."),
    ("Bird of Paradise", "Strelitzia reginae", "Strelitziaceae",
     "Orange and blue flowers that resemble a bird's head.\nStiff, paddle-shaped leaves.",
     "The official flower of the city of Los Angeles."),
    ("Lobster Claw Heliconia", "Heliconia rostrata", "Heliconiaceae",
     "Hanging chains of red bracts edged with yellow.\nLarge banana-like leaves.",
     "SAMPLE: replace with a verified fun fact."),
    ("Angsana", "Pterocarpus indicus", "Fabaceae",
     "Tall tree with a broad crown.\nCompound leaves.\nSmall yellow flowers in sprays.\nFlat, winged pods.",
     "SAMPLE: replace with a verified fun fact."),
    ("Bougainvillea", "Bougainvillea glabra", "Nyctaginaceae",
     "Thorny climber.\nBrightly coloured papery bracts surrounding tiny white flowers.",
     "Named after the French admiral Louis Antoine de Bougainville."),
    ("Hibiscus", "Hibiscus rosa-sinensis", "Malvaceae",
     "Large trumpet-shaped flowers with five petals.\nProminent central column.\nGlossy leaves.",
     "The national flower of Malaysia."),
    ("Sacred Lotus", "Nelumbo nucifera", "Nelumbonaceae",
     "Round leaves held above the water.\nLarge pink or white flowers.\nSeed pod shaped like a shower head.",
     "The seeds can stay alive for a very long time."),
    ("Giant Water Lily", "Victoria amazonica", "Nymphaeaceae",
     "Huge circular floating leaves with an upturned rim.\nSpiny underside.",
     "Named in honour of Queen Victoria."),
    ("Cannonball Tree", "Couroupita guianensis", "Lecythidaceae",
     "Round woody fruits the size of cannonballs hang from the trunk.\nPink-red waxy flowers on long stalks.",
     "The fruits look like cannonballs, which gives the tree its name."),
    ("Banyan", "Ficus benghalensis", "Moraceae",
     "Aerial roots that grow down and thicken into pillar-like trunks.\nVery wide spreading crown.",
     "The national tree of India."),
    ("Red Ginger", "Alpinia purpurata", "Zingiberaceae",
     "Upright reed-like stems.\nBright red bracts in a cone-shaped spike.",
     "SAMPLE: replace with a verified fun fact."),
]

STRUCTURES = [
    "Burkill Hall", "Bandstand", "Botanic Gardens Heritage Museum", "Holttum Hall",
    "Ridley Hall", "Swan Lake Gazebo", "Shaw Foundation Symphony Stage",
    "Palm Valley Pavilion", "Visitor Centre", "Children's Garden Pavilion",
    "Sundial Garden Shelter", "Jelutong Tower", "Tanglin Gate", "Nassim Gate",
    "Rainforest Boardwalk",
]
STYLES = ["Colonial", "Victorian", "Art Deco", "Modernist", "Tropical vernacular",
          "Neo-classical", "Contemporary"]
DATES = ["1868", "1930", "c. 1900", "1950s", "1972", "1995", "2007", "2014"]
LOCATIONS = ["Near Symphony Lake", "Tanglin Gate entrance", "Palm Valley", "Rainforest",
             "Ginger Garden", "Evolution Garden", "Learning Forest", "Eco-Lake"]
DESCRIPTIONS = [
    "Open-sided structure with a pitched roof and timber posts.",
    "Two-storey building with a covered verandah and louvred windows.",
    "Low shelter with a curved roof, set among mature trees.",
    "Raised walkway with railings, built to blend into the surroundings.",
    "Stone gateway with a decorative iron gate.",
]


def placeholder(label, rng):
    """A plain coloured picture with the record name, standing in for a real photo."""
    colour = (rng.randint(40, 150), rng.randint(90, 180), rng.randint(60, 150))
    img = Image.new("RGB", (400, 300), colour)
    ImageDraw.Draw(img).text((16, 16), label, fill=(255, 255, 255))
    buf = io.BytesIO()
    img.save(buf, "PNG")
    return ContentFile(buf.getvalue())


def file_name(label):
    # The "sample-" prefix lets --clear find (and only remove) these records later.
    return "sample-" + "".join(c if c.isalnum() else "-" for c in label.lower()) + ".png"


class Command(BaseCommand):
    help = ("Create 15 SAMPLE botanical and 15 SAMPLE architecture records. "
            "Safe to run twice. Use --clear to remove the sample records.")

    def add_arguments(self, parser):
        parser.add_argument("--clear", action="store_true",
                            help="delete the sample records created by this command, then stop")

    def handle(self, *args, **opts):
        say = self.stdout.write if opts["verbosity"] > 0 else (lambda *_: None)

        if opts["clear"]:
            for model in (BotanicalRecord, ArchitectureRecord):
                deleted, _ = model.all_objects.filter(image__contains="sample-").delete()
                say(f"removed {deleted} sample {model.__name__} rows")
            return

        rng = random.Random(2026)        # same "random" data every time
        pick_status = lambda: rng.choices(
            [BotanicalRecord.PUBLISHED, BotanicalRecord.DRAFT], weights=[2, 1])[0]

        made = 0
        for common, sci, family, features, fact in PLANTS:
            if BotanicalRecord.all_objects.filter(scientific_name=sci).exists():
                say(f"skipped {common}")
                continue
            rec = BotanicalRecord(common_name=common, scientific_name=sci, family=family,
                                  identification_features=features, fun_fact=fact,
                                  status=pick_status())
            rec.image.save(file_name(common), placeholder(common, rng), save=False)
            rec.save()
            made += 1
        say(f"botanical records created: {made}")

        made = 0
        for name in STRUCTURES:
            if ArchitectureRecord.all_objects.filter(name=name).exists():
                say(f"skipped {name}")
                continue
            rec = ArchitectureRecord(
                name=name, location=rng.choice(LOCATIONS),
                architectural_style=rng.choice(STYLES), construction_date=rng.choice(DATES),
                description="SAMPLE: " + rng.choice(DESCRIPTIONS), fun_fact="",
                status=pick_status())
            rec.image.save(file_name(name), placeholder(name, rng), save=False)
            rec.save()
            made += 1
        say(f"architecture records created: {made}")