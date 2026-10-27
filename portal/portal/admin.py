from django.contrib import admin
from .models import AuditLog, CollectibleErrorLog


@admin.register(CollectibleErrorLog)
class CollectibleErrorLogAdmin(admin.ModelAdmin):
    """Lets developers add test rows by hand until the mobile API writes real ones."""
    list_display = ("occurred_at", "error_type", "status", "collectible_name",
                    "visitor_username", "message")
    list_filter = ("error_type", "status")
    search_fields = ("collectible_name", "visitor_username", "message")


@admin.register(AuditLog)
class AuditLogAdmin(admin.ModelAdmin):
    list_display = ("timestamp", "username", "role", "action", "record_type", "details")
    list_filter = ("action", "role")
    search_fields = ("username", "details")

    def has_add_permission(self, request):
        return False

    def has_change_permission(self, request, obj=None):
        return False

    def has_delete_permission(self, request, obj=None):
        return False