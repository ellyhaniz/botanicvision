from django.core.management.base import BaseCommand

from portal.models import Collectible

from .seed_scan_collectibles import (
    SWAN_LAKE_FACT, SWING_ME_MAMA_DESCRIPTION, SWING_ME_MAMA_FACT, TANGLIN_GATE_BUILT,
    TANGLIN_GATE_STYLE, VANDA_FACT,
)

# scan label -> fields to set on its record, and the card's own description (or None to keep it).
FIXES = {
    "vanda_hybrid": ({"fun_fact": VANDA_FACT}, None),
    "swan_lake": ({"fun_fact": SWAN_LAKE_FACT}, None),
    "swing_me_mama": ({"location": "Near Swan Lake", "construction_date": "1999",
                       "description": SWING_ME_MAMA_DESCRIPTION, "fun_fact": SWING_ME_MAMA_FACT},
                      SWING_ME_MAMA_DESCRIPTION),
    "tanglin_gate": ({"architectural_style": TANGLIN_GATE_STYLE, "construction_date": TANGLIN_GATE_BUILT},
                     None),
}


class Command(BaseCommand):
    help = ("Replace the SAMPLE placeholder text on the Vanda Orchid, Swan Lake, Swing Me Mama and "
            "Tanglin Gate cards with checked facts. Changes only those fields. Safe to run twice.")

    def handle(self, *args, **opts):
        for label, (record_fields, card_description) in FIXES.items():
            card = Collectible.objects.filter(model_label=label).first()
            record = card and (card.botanical_record or card.architecture_record)
            if record is None:
                self.stdout.write(f"  {label:16} no card found, skipped")
                continue
            for field, value in record_fields.items():
                setattr(record, field, value)
            record.save(update_fields=[*record_fields, "updated_at"])
            if card_description is not None:
                card.description = card_description
                card.save(update_fields=["description", "updated_at"])
            self.stdout.write(f"  {label:16} updated '{card.name}'")
