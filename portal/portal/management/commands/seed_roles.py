from django.contrib.auth.models import Group, User
from django.core.management.base import BaseCommand

from portal.models import Feature, ProfileType
from portal.roles import ROLES, SECTIONS

DEMO = {"sysadmin": "System Admin", "useradmin": "User Admin", "researcher": "Researcher"}
NAMES = {"sysadmin": ("Sam", "Admin"), "useradmin": ("Uma", "Admin"), "researcher": ("Riya", "Researcher")}

DESCRIPTIONS = {
    "System Admin": "Manages discovery collectibles and monitors error logs.",
    "User Admin": "Manages staff accounts and profile types.",
    "Researcher": "Maintains botanical and architectural records.",
}


class Command(BaseCommand):
    help = "Create role groups, profile types, features and demo staff accounts"

    def handle(self, *args, **opts):
        # 1. Features (one per portal section). Remember which are brand new.
        new_features = []
        for slug, d in SECTIONS.items():
            feature, created = Feature.objects.update_or_create(
                code=slug, defaults={"title": d["title"]})
            if created:
                new_features.append(feature)

        # 2. Role groups and profile types
        for r in ROLES:
            group, _ = Group.objects.get_or_create(name=r)
            pt, created = ProfileType.objects.get_or_create(
                group=group, defaults={"description": DESCRIPTIONS[r]})
            defaults = Feature.objects.filter(
                code__in=[s for s, d in SECTIONS.items() if r in d["roles"]])
            if created:
                # First run: give the profile type its default permissions.
                pt.permissions.set(defaults)
            else:
                # Later runs: only grant sections that did not exist before, so a new
                # section appears for the right role, but permissions a User Admin
                # has edited are never overwritten.
                pt.permissions.add(*[f for f in defaults if f in new_features])

        for username, role in DEMO.items():
            user, created = User.objects.get_or_create(username=username)
            if created:                              # don't reset passwords on later runs
                user.set_password("ChangeMe123!")
            if not user.email:
                user.email = f"{username}@sbg.test"
            if not user.first_name:
                user.first_name, user.last_name = NAMES[username]
            user.save()
            user.groups.set([Group.objects.get(name=role)])
            if opts["verbosity"] > 0:
                self.stdout.write(f"{username} -> {role}")
