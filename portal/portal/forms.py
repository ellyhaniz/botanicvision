import re

from django import forms
from django.contrib.auth.models import Group, User
from django.core.files.uploadedfile import UploadedFile

from .models import ArchitectureRecord, BotanicalRecord, Feature, ProfileType
from .roles import ROLES


class AccountForm(forms.ModelForm):    
    profile_type = forms.ModelChoiceField(
        queryset=Group.objects.filter(name__in=ROLES).order_by("name"),
        required=False, empty_label="Visitor (no staff access)", label="Profile type",
    )

    class Meta:
        model = User
        fields = ["first_name", "last_name", "email"]
        labels = {"first_name": "First name", "last_name": "Last name", "email": "Email"}

    def __init__(self, *args, lock_profile=False, current_role=None, **kwargs):
        super().__init__(*args, **kwargs)
        for name in ("first_name", "last_name", "email"):
            self.fields[name].required = True
        self.fields["profile_type"].initial = Group.objects.filter(name=current_role).first()
        if lock_profile:                        # editing your own account
            self.fields["profile_type"].disabled = True

    def clean_email(self):
        email = self.cleaned_data["email"].strip().lower()
        clash = User.objects.filter(email__iexact=email).exclude(pk=self.instance.pk)
        if clash.exists():
            raise forms.ValidationError("Another account already uses this email.")
        return email

class ProfileTypeForm(forms.ModelForm):
    permissions = forms.ModelMultipleChoiceField(
        queryset=Feature.objects.all(), required=False,
        widget=forms.CheckboxSelectMultiple, label="Permissions",
    )

    class Meta:
        model = ProfileType
        fields = ["description", "permissions"]
        widgets = {"description": forms.Textarea(attrs={"rows": 3})}

    # Permissions the User Admin profile type must always keep, otherwise
    # nobody could repair the system from the portal.
    PROTECTED = {"User Admin": {"accounts": "User Account Management",
                                "profile-types": "Profile Type Management"}}

    def clean_permissions(self):
        perms = self.cleaned_data["permissions"]
        for code, title in self.PROTECTED.get(self.instance.group.name, {}).items():
            if not perms.filter(code=code).exists():
                raise forms.ValidationError(
                    f"User Admin must keep access to {title}, otherwise nobody "
                    "could manage accounts or profile types from the portal.")
        return perms


# ---------------------------------------------------------------------------
# Researcher records
# ---------------------------------------------------------------------------
MAX_IMAGE_MB = 5
ALLOWED_IMAGE_EXTENSIONS = {"jpg", "jpeg", "png", "webp"}


class LineBreakTextarea(forms.Textarea):
    # Browsers send line breaks as \r\n, but we store \n. Without this, every edit
    # looks like the text changed (false audit entries, "no changes" never detected).
    def value_from_datadict(self, data, files, name):
        value = super().value_from_datadict(data, files, name)
        if isinstance(value, str):
            return value.replace("\r\n", "\n").replace("\r", "\n")
        return value


def tidy(value):
    """Trim and collapse repeated spaces, so 'Ficus   elastica ' == 'Ficus elastica'."""
    return " ".join((value or "").split())


def tidy_block(value):
    """Like tidy(), but keeps line breaks (for multi-line text boxes)."""
    lines = [" ".join(line.split()) for line in (value or "").strip().splitlines()]
    out, blank = [], False
    for line in lines:
        if line:
            out.append(line)
            blank = False
        elif not blank:          # keep at most one blank line in a row
            out.append("")
            blank = True
    return "\n".join(out)


class RecordForm(forms.ModelForm):
    """Shared behaviour for botanical and architecture forms."""

    status = forms.ChoiceField(
        choices=BotanicalRecord.STATUSES, initial=BotanicalRecord.PUBLISHED,
        help_text="Draft records are still being checked. Only Published records "
                  "should be shown to visitors or linked to a collectible.")

    def clean_image(self):
        image = self.cleaned_data.get("image")
        # A new upload is an UploadedFile. When editing and no file is chosen,
        # Django hands back the existing stored file, which needs no re-check.
        if isinstance(image, UploadedFile):
            ext = image.name.rsplit(".", 1)[-1].lower() if "." in image.name else ""
            if ext not in ALLOWED_IMAGE_EXTENSIONS:
                raise forms.ValidationError("Upload a JPG, PNG or WebP image.")
            if image.size > MAX_IMAGE_MB * 1024 * 1024:
                raise forms.ValidationError(f"The image must be {MAX_IMAGE_MB} MB or smaller.")
        return image


class BotanicalRecordForm(RecordForm):
    class Meta:
        model = BotanicalRecord
        fields = ["common_name", "scientific_name", "family",
                  "identification_features", "fun_fact", "image", "status"]
        labels = {"common_name": "Common name", "scientific_name": "Scientific name",
                  "family": "Family", "identification_features": "Identification features",
                  "fun_fact": "Fun fact", "image": "Reference image"}
        widgets = {
            "identification_features": LineBreakTextarea(attrs={"rows": 4}),
            "fun_fact": LineBreakTextarea(attrs={"rows": 3}),
            "image": forms.FileInput(attrs={"accept": "image/jpeg,image/png,image/webp"}),
        }
        help_texts = {
            "scientific_name": "For example: Cyrtophyllum fragrans",
            "identification_features": "What makes it recognisable (bark, leaves, flowers, shape).",
            "image": "JPG, PNG or WebP, up to 5 MB. When editing, leave empty to keep the current image.",
        }

    def clean_common_name(self):
        return tidy(self.cleaned_data["common_name"])

    def clean_family(self):
        return tidy(self.cleaned_data["family"])

    def clean_scientific_name(self):
        name = tidy(self.cleaned_data["scientific_name"])
        if not re.fullmatch(r"[A-Za-z][A-Za-z .\-'×]*", name):
            raise forms.ValidationError(
                "Use letters only (spaces, hyphens, apostrophes and × are allowed).")
        return name

    def clean_identification_features(self):
        return tidy_block(self.cleaned_data["identification_features"])

    def clean_fun_fact(self):
        return tidy_block(self.cleaned_data["fun_fact"])


class ArchitectureRecordForm(RecordForm):
    class Meta:
        model = ArchitectureRecord
        fields = ["name", "location", "architectural_style", "construction_date",
                  "description", "fun_fact", "image", "status"]
        labels = {"name": "Structure name", "location": "Location in the Gardens",
                  "architectural_style": "Architectural style",
                  "construction_date": "Construction date", "description": "Description",
                  "fun_fact": "Fun fact (optional)", "image": "Reference image"}
        widgets = {
            "description": LineBreakTextarea(attrs={"rows": 4}),
            "fun_fact": LineBreakTextarea(attrs={"rows": 3}),
            "image": forms.FileInput(attrs={"accept": "image/jpeg,image/png,image/webp"}),
        }
        help_texts = {
            "construction_date": "A year or date, for example: 1868 or c. 1900",
            "description": "Notable features that help identify the structure.",
            "image": "JPG, PNG or WebP, up to 5 MB. When editing, leave empty to keep the current image.",
        }

    def clean_name(self):
        return tidy(self.cleaned_data["name"])

    def clean_location(self):
        return tidy(self.cleaned_data["location"])

    def clean_architectural_style(self):
        return tidy(self.cleaned_data["architectural_style"])

    def clean_construction_date(self):
        return tidy(self.cleaned_data["construction_date"])

    def clean_description(self):
        return tidy_block(self.cleaned_data["description"])

    def clean_fun_fact(self):
        return tidy_block(self.cleaned_data["fun_fact"])
