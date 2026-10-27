from functools import wraps

from django.contrib.auth.views import redirect_to_login
from django.core.exceptions import PermissionDenied

from .roles import get_role


def role_required(*allowed_roles):
    """Login required, and the user's role must be one of allowed_roles."""
    def decorator(view):
        @wraps(view)
        def wrapper(request, *args, **kwargs):
            if not request.user.is_authenticated:
                return redirect_to_login(request.get_full_path())
            if get_role(request.user) not in allowed_roles:
                raise PermissionDenied          # 403 page
            return view(request, *args, **kwargs)
        return wrapper
    return decorator

from .roles import allowed_slugs


def feature_required(slug):
    """Login required, and the user's profile type must include this section."""
    def decorator(view):
        @wraps(view)
        def wrapper(request, *args, **kwargs):
            if not request.user.is_authenticated:
                return redirect_to_login(request.get_full_path())
            if slug not in allowed_slugs(request.user):
                raise PermissionDenied
            return view(request, *args, **kwargs)
        return wrapper
    return decorator