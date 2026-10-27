from .roles import get_role


def role(request):
    """Makes `role` available in every template (used for the badge in the top bar)."""
    user = getattr(request, "user", None)
    return {"role": get_role(user) if user is not None and user.is_authenticated else None}
