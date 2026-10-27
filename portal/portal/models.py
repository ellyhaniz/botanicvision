from django.contrib.auth.models import Group
from django.conf import settings
from django.db import models
from django.utils import timezone


class AuditLog(models.Model):
    LOGIN = "LOGIN"
    LOGOUT = "LOGOUT"
    LOGIN_FAILED = "LOGIN_FAILED"
    CREATE = "CREATE"
    UPDATE = "UPDATE"
    DELETE = "DELETE"
    ACTIONS = [
        (LOGIN, "Log in"),
        (LOGOUT, "Log out"),
        (LOGIN_FAILED, "Failed log in"),
        (CREATE, "Create"),
        (UPDATE, "Update"),
        (DELETE, "Delete"),
    ]

    # Who. The username and role are copied in as text so the entry still
    # makes sense even if the account is later renamed or deleted.
    user = models.ForeignKey(
        settings.AUTH_USER_MODEL, null=True, blank=True,
        on_delete=models.SET_NULL, related_name="audit_logs",
    )
    username = models.CharField(max_length=150)
    role = models.CharField(max_length=50, blank=True)

    # What
    action = models.CharField(max_length=20, choices=ACTIONS)
    record_type = models.CharField(max_length=50, blank=True)   # e.g. "Botanical Record"
    details = models.CharField(max_length=255, blank=True)      # e.g. "Tembusu"

    # Where / when
    ip_address = models.GenericIPAddressField(null=True, blank=True)
    timestamp = models.DateTimeField(auto_now_add=True, db_index=True)

    class Meta:
        ordering = ["-timestamp"]

    def __str__(self):
        return f"{self.timestamp:%Y-%m-%d %H:%M} {self.username} {self.action}"

    # Read-only: existing entries cannot be edited or deleted through Django
    def save(self, *args, **kwargs):
        if not self._state.adding:
            raise PermissionError("Audit log entries are read-only.")
        super().save(*args, **kwargs)

    def delete(self, *args, **kwargs):
        raise PermissionError("Audit log entries cannot be deleted.")

class Feature(models.Model):
    """One portal section a profile type can be given access to."""
    code = models.SlugField(unique=True)          # matches the slug in roles.SECTIONS
    title = models.CharField(max_length=100)

    class Meta:
        ordering = ["title"]

    def __str__(self):
        return self.title


class ProfileType(models.Model):
    group = models.OneToOneField(Group, on_delete=models.CASCADE, related_name="profile_type")
    description = models.CharField(max_length=255, blank=True)
    permissions = models.ManyToManyField(Feature, blank=True, related_name="profile_types")
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        ordering = ["group__name"]

    def __str__(self):
        return self.group.name


class CollectibleErrorLog(models.Model):
    """One failed card unlock or handbook sync, reported by the mobile app (SA-09).

    The mobile API writes these rows; the portal only reads them. The collectible and
    visitor are stored as text so an entry still makes sense after the collectible is
    deleted, and because visitor accounts live with the mobile app, not in auth_user.
    """
    UNLOCK_FAILED = "UNLOCK_FAILED"
    SYNC_FAILED = "SYNC_FAILED"
    OTHER = "OTHER"
    ERROR_TYPES = [
        (UNLOCK_FAILED, "Card unlock failed"),
        (SYNC_FAILED, "Handbook sync failed"),
        (OTHER, "Other"),
    ]

    OPEN = "OPEN"
    INVESTIGATING = "INVESTIGATING"
    RESOLVED = "RESOLVED"
    STATUSES = [
        (OPEN, "Open"),
        (INVESTIGATING, "Investigating"),
        (RESOLVED, "Resolved"),
    ]

    error_type = models.CharField(max_length=20, choices=ERROR_TYPES, db_index=True)
    status = models.CharField(max_length=20, choices=STATUSES, default=OPEN, db_index=True)
    collectible_name = models.CharField(max_length=150, blank=True, db_index=True)
    visitor_username = models.CharField(max_length=150, blank=True, db_index=True)
    message = models.CharField(max_length=255)                  # short summary for the list
    details = models.TextField(blank=True)                      # full error text / stack trace
    device_info = models.CharField(max_length=255, blank=True)  # e.g. "Pixel 7, Android 15, app 1.2.0"
    occurred_at = models.DateTimeField(default=timezone.now, db_index=True)

    class Meta:
        ordering = ["-occurred_at"]

    def __str__(self):
        return f"{self.occurred_at:%Y-%m-%d %H:%M} {self.error_type} {self.collectible_name}"