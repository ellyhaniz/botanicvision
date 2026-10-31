from datetime import timedelta

from django.contrib.auth.models import Group, User
from django.core.management.base import BaseCommand
from django.utils import timezone

from ...models import CollectibleErrorLog

PASSWORD = "ChangeMe123!"

DEMO_COLLECTIBLES = ["Tembusu", "Orchid (Vanda Miss Joaquim)", "Rain Tree",
                     "Burkill Hall", "Swan Lake Gazebo", "Bandstand"]
DEMO_ERRORS = [
    (CollectibleErrorLog.UNLOCK_FAILED, "Plant not recognised (confidence 0.41 below 0.70)"),
    (CollectibleErrorLog.UNLOCK_FAILED, "Visitor outside the collectible's GPS radius"),
    (CollectibleErrorLog.UNLOCK_FAILED, "Camera image too dark to classify"),
    (CollectibleErrorLog.SYNC_FAILED, "Handbook sync timed out after 30 s"),
    (CollectibleErrorLog.SYNC_FAILED, "Server returned 500 while saving unlocked card"),
    (CollectibleErrorLog.OTHER, "Card image failed to download"),
]


class Command(BaseCommand):
    help = ("Create extra TEST data: accounts (12 researchers, 8 visitors, 1 suspended staff) "
            "and 30 collectible error logs, so list, search, paging and filters can be tried. "
            "Safe to run twice.")

    def _say(self, msg):
        if self.verbosity > 0:
            self.stdout.write(msg)

    def _make(self, username, first, last, group=None, active=True):
        if User.objects.filter(username=username).exists():
            self._say(f"skipped {username}")
            return
        user = User.objects.create_user(
            username, f"{username}@sbg.test", PASSWORD,
            first_name=first, last_name=last, is_active=active)
        if group:
            user.groups.add(Group.objects.get(name=group))
        self._say(f"created {username} -> {group or 'Visitor'}"
                  + ("" if active else " (suspended)"))

    def handle(self, *args, **opts):
        self.verbosity = opts["verbosity"]
        if not Group.objects.filter(name="Researcher").exists():
            self.stderr.write("Run `python manage.py seed_roles` first.")
            return
        for i in range(1, 13):
            self._make(f"staff{i:02d}", "Staff", f"Member{i}", group="Researcher")
        for i in range(1, 9):
            self._make(f"visitor{i:02d}", "Visitor", f"Guest{i}", active=(i != 8))
        self._make("staff_suspended", "Old", "Researcher", group="Researcher", active=False)
        self._make_error_logs()

    def _make_error_logs(self):
        if CollectibleErrorLog.objects.exists():
            self._say("skipped error logs (some already exist)")
            return
        now = timezone.now()
        statuses = [CollectibleErrorLog.OPEN] * 3 + [CollectibleErrorLog.INVESTIGATING,
                                                      CollectibleErrorLog.RESOLVED]
        for i in range(30):
            error_type, message = DEMO_ERRORS[i % len(DEMO_ERRORS)]
            CollectibleErrorLog.objects.create(
                error_type=error_type,
                status=statuses[i % len(statuses)],
                collectible_name=DEMO_COLLECTIBLES[i % len(DEMO_COLLECTIBLES)],
                visitor_username=f"visitor{i % 8 + 1:02d}",
                message=message,
                details=f"Demo entry {i + 1}. {message}.",
                device_info=["Pixel 7, Android 15", "Galaxy S23, Android 14"][i % 2],
                occurred_at=now - timedelta(hours=7 * i),
            )
        self._say("created 30 demo error logs")
