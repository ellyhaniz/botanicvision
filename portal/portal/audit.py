from .models import AuditLog
from .roles import get_role


def get_client_ip(request):
    if request is None:
        return None
    forwarded = request.META.get("HTTP_X_FORWARDED_FOR")
    if forwarded:
        return forwarded.split(",")[0].strip()
    return request.META.get("REMOTE_ADDR")


def log_action(user, action, record_type="", details="", request=None, username=""):
    """Write one audit entry. `user` may be None (e.g. a failed login)."""
    real_user = user if (user is not None and user.is_authenticated) else None
    AuditLog.objects.create(
        user=real_user,
        username=real_user.get_username() if real_user else username,
        role=(get_role(real_user) or "") if real_user else "",
        action=action,
        record_type=record_type,
        details=details[:255],
        ip_address=get_client_ip(request),
    )
