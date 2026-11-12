from django.contrib.auth.models import Group
from django.conf import settings
from django.db import models
from django.db.models import Q
from django.utils import timezone

from .storage import database_storage


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


# ---------------------------------------------------------------------------
# Researcher records (RS-04 .. RS-13)
# ---------------------------------------------------------------------------
class ActiveManager(models.Manager):
    """Default manager: hides soft-deleted records everywhere (lists, search, detail)."""
    def get_queryset(self):
        return super().get_queryset().filter(is_deleted=False)


class RecordBase(models.Model):
    """Fields shared by botanical and architecture records."""
    DRAFT = "DRAFT"
    PUBLISHED = "PUBLISHED"
    STATUSES = [(DRAFT, "Draft"), (PUBLISHED, "Published")]

    # Draft = still being checked. Only Published records should be shown to
    # visitors or linked to a collectible (risk R01).
    status = models.CharField(max_length=10, choices=STATUSES, default=PUBLISHED)

    # Soft delete (risk R09): the row is kept, but hidden everywhere.
    is_deleted = models.BooleanField(default=False, db_index=True)
    deleted_at = models.DateTimeField(null=True, blank=True)

    created_by = models.ForeignKey(
        settings.AUTH_USER_MODEL, null=True, blank=True,
        on_delete=models.SET_NULL, related_name="+",
    )
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    objects = ActiveManager()        # normal queries: not deleted
    all_objects = models.Manager()   # includes soft-deleted (admin / recovery)

    class Meta:
        abstract = True

    def soft_delete(self):
        self.is_deleted = True
        self.deleted_at = timezone.now()
        self.save(update_fields=["is_deleted", "deleted_at", "updated_at"])


class BotanicalRecord(RecordBase):
    common_name = models.CharField(max_length=100, db_index=True)
    scientific_name = models.CharField(max_length=150, db_index=True)
    family = models.CharField(max_length=100, db_index=True)
    image = models.ImageField(upload_to="botanical/%Y/%m/", storage=database_storage)
    identification_features = models.TextField()
    fun_fact = models.TextField()

    class Meta:
        ordering = ["common_name"]

    def __str__(self):
        return self.common_name


class ArchitectureRecord(RecordBase):
    name = models.CharField(max_length=150, db_index=True)
    image = models.ImageField(upload_to="architecture/%Y/%m/", storage=database_storage)
    location = models.CharField(max_length=150, db_index=True)
    architectural_style = models.CharField(max_length=100)
    # Free text on purpose: heritage dates are often a year or "c. 1900".
    construction_date = models.CharField(max_length=50)
    description = models.TextField()
    fun_fact = models.TextField(blank=True)   # optional

    class Meta:
        ordering = ["name"]

    def __str__(self):
        return self.name


# ---------------------------------------------------------------------------
# Discovery collectibles (SA-04 .. SA-08)
# ---------------------------------------------------------------------------
class StoredFile(models.Model):
    """A file kept in the database. See portal/storage.py for why."""
    name = models.CharField(max_length=255, unique=True)   # e.g. collectibles/2026/10/tembusu.png
    content = models.BinaryField()
    content_type = models.CharField(max_length=100, blank=True)
    size = models.PositiveIntegerField(default=0)
    created_at = models.DateTimeField(auto_now_add=True)

    def __str__(self):
        return self.name


class CollectibleCategory(models.Model):
    """Groups collectibles for searching and filtering, e.g. Flora, Architecture."""
    name = models.CharField(max_length=60, unique=True)
    description = models.CharField(max_length=255, blank=True)

    class Meta:
        ordering = ["name"]
        verbose_name_plural = "collectible categories"

    def __str__(self):
        return self.name


class Collectible(models.Model):
    """A discovery card a visitor unlocks by identifying one verified plant or structure."""
    COMMON, UNCOMMON, RARE, HIDDEN = "COMMON", "UNCOMMON", "RARE", "HIDDEN"
    RARITIES = [(COMMON, "Common"), (UNCOMMON, "Uncommon"), (RARE, "Rare"), (HIDDEN, "Hidden")]

    DRAFT, PUBLISHED = "DRAFT", "PUBLISHED"
    STATUSES = [(DRAFT, "Draft"), (PUBLISHED, "Published")]

    name = models.CharField(max_length=100, db_index=True)
    description = models.TextField()
    image = models.ImageField(upload_to="collectibles/%Y/%m/", storage=database_storage)
    category = models.ForeignKey(CollectibleCategory, on_delete=models.PROTECT,
                                 related_name="collectibles")
    rarity = models.CharField(max_length=10, choices=RARITIES, default=COMMON, db_index=True)
    status = models.CharField(max_length=10, choices=STATUSES, default=PUBLISHED, db_index=True)

    # The class name the PlantDex image model outputs for this plant or structure
    # (a line of ml/labels.txt, e.g. "tanglin_gate"). A confident scan with this label
    # unlocks the card. Empty = this card can't be unlocked by scanning yet.
    model_label = models.CharField(max_length=60, blank=True, db_index=True)

    # Exactly ONE of these is set (enforced below). OneToOne means a record can have at
    # most one collectible, which is the "duplicate collectible" rule of SA-04 / SA-06.
    # PROTECT stops a linked record from being hard-deleted by accident.
    botanical_record = models.OneToOneField(
        BotanicalRecord, null=True, blank=True, on_delete=models.PROTECT, related_name="collectible")
    architecture_record = models.OneToOneField(
        ArchitectureRecord, null=True, blank=True, on_delete=models.PROTECT, related_name="collectible")

    created_by = models.ForeignKey(settings.AUTH_USER_MODEL, null=True, blank=True,
                                   on_delete=models.SET_NULL, related_name="+")
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        ordering = ["name"]
        constraints = [
            models.CheckConstraint(
                condition=(Q(botanical_record__isnull=False, architecture_record__isnull=True)
                           | Q(botanical_record__isnull=True, architecture_record__isnull=False)),
                name="collectible_links_exactly_one_record"),
            # "> ''" rather than "not empty": SQL Server's filtered indexes don't allow NOT
            models.UniqueConstraint(fields=["model_label"], condition=Q(model_label__gt=""),
                                    name="one_collectible_per_model_label"),
        ]

    def __str__(self):
        return self.name

    # --- the linked record, whichever kind it is -------------------------
    @property
    def linked_record(self):
        return self.botanical_record or self.architecture_record

    @property
    def linked_kind(self):
        return "Botanical" if self.botanical_record_id else "Architecture"

    @property
    def linked_name(self):
        record = self.linked_record
        if record is None:
            return ""
        return record.common_name if self.botanical_record_id else record.name

    @property
    def linked_available(self):
        """False if the linked record was deleted or is no longer verified (Published)."""
        record = self.linked_record
        return record is not None and not record.is_deleted and record.status == "PUBLISHED"


class CollectibleUnlock(models.Model):
    """A visitor unlocked a collectible. Written by the mobile app's API, read here.

    It is what makes a collectible "in use": one with unlocks cannot be deleted (SA-07).
    Visitor accounts live with the mobile app, so the visitor is stored as text.
    """
    collectible = models.ForeignKey(Collectible, on_delete=models.PROTECT, related_name="unlocks")
    visitor_username = models.CharField(max_length=150, db_index=True)
    unlocked_at = models.DateTimeField(default=timezone.now)

    class Meta:
        ordering = ["-unlocked_at"]
        constraints = [
            models.UniqueConstraint(fields=["collectible", "visitor_username"],
                                    name="one_unlock_per_visitor_per_collectible"),
        ]

    def __str__(self):
        return f"{self.visitor_username} unlocked {self.collectible_id}"

