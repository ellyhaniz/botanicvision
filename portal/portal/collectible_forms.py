from django import forms

from .forms import LineBreakTextarea, RecordForm, tidy, tidy_block
from .models import (ArchitectureRecord, BotanicalRecord, Collectible, CollectibleCategory)


class CollectibleForm(RecordForm):
    """SA-04 / SA-06. Inherits the image checks (type, size) and the status field from RecordForm."""

    status = forms.ChoiceField(
        choices=Collectible.STATUSES, initial=Collectible.PUBLISHED,
        help_text="Draft collectibles are hidden from visitors.")

    linked_record = forms.ChoiceField(
        label="Linked record",
        error_messages={"required": "Choose the record this collectible is linked to.",
                        "invalid_choice": "That record no longer exists. Choose another one."},
        help_text="The plant or structure a visitor must identify to unlock this card. "
                  "Only Published (verified) records can be linked, and each record can have "
                  "only one collectible.")

    class Meta:
        model = Collectible
        fields = ["name", "category", "rarity", "description", "image", "status"]
        labels = {"name": "Collectible name", "category": "Category", "rarity": "Rarity level",
                  "description": "Description", "image": "Card image"}
        widgets = {
            "description": LineBreakTextarea(attrs={"rows": 4}),
            "image": forms.FileInput(attrs={"accept": "image/jpeg,image/png,image/webp"}),
        }
        help_texts = {
            "image": "JPG, PNG or WebP, up to 5 MB. When editing, leave empty to keep the current image.",
        }

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.fields["category"].queryset = CollectibleCategory.objects.all()
        self.fields["category"].empty_label = "Choose a category"
        self.fields["linked_record"].choices = self._record_choices()
        if self.instance.pk:                      # editing: preselect the current link
            kind = "b" if self.instance.botanical_record_id else "a"
            self.initial["linked_record"] = f"{kind}:{self.instance.linked_record.pk}"
        self.order_fields(["name", "linked_record", "category", "rarity",
                           "description", "image", "status"])

    # ---- the drop-down ----------------------------------------------------
    def _record_choices(self):
        """Every existing record, with a marker when it cannot be chosen (the use-case
        messages are produced on submit, so the person sees *why*)."""
        own_b, own_a = self.instance.botanical_record_id, self.instance.architecture_record_id
        taken_b = set(Collectible.objects.exclude(botanical_record=None)
                      .exclude(botanical_record_id=own_b).values_list("botanical_record_id", flat=True))
        taken_a = set(Collectible.objects.exclude(architecture_record=None)
                      .exclude(architecture_record_id=own_a).values_list("architecture_record_id", flat=True))

        def label(text, record, taken):
            if record.status != "PUBLISHED":
                return f"{text}  (Draft, not verified)"
            if record.pk in taken:
                return f"{text}  (already has a collectible)"
            return text

        botanical = [(f"b:{r.pk}", label(f"{r.common_name} ({r.scientific_name})", r, taken_b))
                     for r in BotanicalRecord.objects.order_by("common_name")]
        architecture = [(f"a:{r.pk}", label(r.name, r, taken_a))
                        for r in ArchitectureRecord.objects.order_by("name")]
        return [("", "Choose a record")] + (
            [("Botanical records", botanical)] if botanical else []) + (
            [("Architecture records", architecture)] if architecture else [])

    # ---- validation (the alternative flows of SA-04 / SA-06) ---------------
    def clean_linked_record(self):
        value = self.cleaned_data["linked_record"]
        kind, _, pk = value.partition(":")
        model = {"b": BotanicalRecord, "a": ArchitectureRecord}.get(kind)
        record = model.objects.filter(pk=pk).first() if model and pk.isdigit() else None
        if record is None:
            raise forms.ValidationError("That record no longer exists. Choose another one.")
        if record.status != "PUBLISHED":
            raise forms.ValidationError(
                "This record is not verified yet (it is a Draft). Ask a Researcher to publish it first.")
        existing = Collectible.objects.filter(**{("botanical_record" if kind == "b" else "architecture_record"): record})
        if self.instance.pk:
            existing = existing.exclude(pk=self.instance.pk)
        if existing.exists():
            raise forms.ValidationError(
                f"A collectible already exists for this record: '{existing.first().name}'.")
        return kind, record

    def clean_name(self):
        return tidy(self.cleaned_data["name"])

    def clean_description(self):
        return tidy_block(self.cleaned_data["description"])

    @property
    def link_changed(self):
        return "linked_record" in self.changed_data

    # ---- saving ----------------------------------------------------------
    def save(self, commit=True):
        obj = super().save(commit=False)
        kind, record = self.cleaned_data["linked_record"]
        obj.botanical_record = record if kind == "b" else None
        obj.architecture_record = record if kind == "a" else None
        if commit:
            obj.save()
        return obj
