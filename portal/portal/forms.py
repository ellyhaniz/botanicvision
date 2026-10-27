from django import forms
from django.contrib.auth.models import Group, User

from .models import Feature, ProfileType
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
