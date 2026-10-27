ROLES = ["System Admin", "User Admin", "Researcher"]

# slug -> title, description, and which roles may open it
SECTIONS = {
    "accounts":      {"title": "User Account Management", "desc": "View, search, update and suspend accounts.", "roles": ["User Admin"]},
    "profile-types": {"title": "Profile Type Management", "desc": "Manage role definitions and permissions.", "roles": ["User Admin"]},
    "collectibles":  {"title": "Collectible Management",  "desc": "Create, update and delete discovery collectibles.", "roles": ["System Admin"]},
    "error-logs":    {"title": "Error Logs",              "desc": "Failed card unlocks and handbook sync issues.", "roles": ["System Admin"]},
    "botanical":     {"title": "Botanical Records",       "desc": "Maintain plant records.", "roles": ["Researcher"]},
    "architecture":  {"title": "Architecture Records",    "desc": "Maintain heritage structure records.", "roles": ["Researcher"]},
    "audit-logs":    {"title": "Audit Logs",              "desc": "Read-only record of administrative actions.", "roles": ROLES},
}

def get_role(user):
    if user.is_superuser:
        return "System Admin"
    group = user.groups.filter(name__in=ROLES).first()
    return group.name if group else None

def allowed_slugs(user):
    """Set of section slugs this user may open, read from the database."""
    if user.is_superuser:
        return set(SECTIONS)
    role = get_role(user)
    if role is None:
        return set()
    from .models import ProfileType            # imported here to avoid a circular import
    pt = ProfileType.objects.filter(group__name=role).first()
    if pt is None:                              # not seeded yet: fall back to the static table
        return {s for s, d in SECTIONS.items() if role in d["roles"]}
    return set(pt.permissions.values_list("code", flat=True))