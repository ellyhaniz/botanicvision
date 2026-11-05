"""
JSON API for the PlantDex Android app (replaces the old Flask backend in /backend).

The paths and JSON shapes match what the app's ApiClient.java already sends and
reads, so the app only needs its BASE_URL pointed at this server.

Accounts are ordinary Django users: a Visitor is a user with no staff group
(the same rule the portal's tests use), and Researcher / User Admin / System
Admin are the existing role groups. Like the Flask version, there is no token
auth yet: the app identifies itself by userId, so treat this as a prototype.
"""
import json
import logging
import os

import requests
from django.contrib.auth.models import User
from django.http import Http404, HttpResponse, JsonResponse
from django.urls import path
from django.views.decorators.csrf import csrf_exempt
from django.views.decorators.http import require_http_methods

from . import classifier
from .audit import log_action
from .models import AuditLog, Collectible, CollectibleUnlock, StoredFile
from .roles import get_role

logger = logging.getLogger(__name__)

# Portal group name -> role code the app routes on (see Roles.java)
ROLE_CODES = {"Researcher": "RESEARCHER", "User Admin": "USER ADMIN", "System Admin": "SYSTEM ADMIN"}

# Below this top-class probability our model's answer is not trusted (it always picks
# one of its classes, even for a photo of something it was never trained on).
MIN_MODEL_CONFIDENCE = float(os.environ.get("PLANTDEX_MIN_CONFIDENCE", "0.6"))


def _body(request):
    try:
        return json.loads(request.body or b"{}")
    except ValueError:
        return {}


def _error(message, status):
    return JsonResponse({"message": message}, status=status)


def _role_code(user):
    role = get_role(user)
    return ROLE_CODES.get(role, "VISITOR")


def _user_json(user):
    group = user.groups.first()
    return {
        "userId": user.pk,
        "username": user.username,
        "role": _role_code(user),
        "profileTypeId": group.pk if group else 0,
        "profileTypeName": get_role(user) or "Visitor",
        "email": user.email,
        "fullName": user.get_full_name(),
        "accountStatus": "ACTIVE" if user.is_active else "INACTIVE",
        "memberSince": user.date_joined.strftime("%d %b %Y"),
    }


def _split_name(full_name):
    first, _, last = full_name.partition(" ")
    return first[:150], last[:150]


# ------------------------------------------------------------------ auth --
@csrf_exempt
@require_http_methods(["POST"])
def register(request):
    """Public self-registration: always creates a Visitor account."""
    data = _body(request)
    email = (data.get("email") or "").strip()
    full_name = (data.get("fullName") or "").strip()
    username = (data.get("username") or "").strip()
    password = data.get("password") or ""

    if not all([email, full_name, username, password]):
        return _error("Please fill in every field", 400)
    if User.objects.filter(username__iexact=username).exists():
        return _error("That username is already taken", 409)

    first, last = _split_name(full_name)
    user = User.objects.create_user(username, email, password, first_name=first, last_name=last)
    log_action(user, AuditLog.CREATE, "Account", f"{username} registered a Visitor account", request)
    return JsonResponse({"message": "Account created"}, status=201)


@csrf_exempt
@require_http_methods(["POST"])
def login(request):
    data = _body(request)
    username = (data.get("username") or "").strip()
    password = data.get("password") or ""

    if not username or not password:
        return _error("Username and password are required", 400)

    user = User.objects.filter(username=username).first()
    if user is None or not user.check_password(password):
        log_action(None, AuditLog.LOGIN_FAILED, details="Mobile app", request=request, username=username)
        return _error("Invalid username or password", 401)
    if not user.is_active:
        return _error("This account has been deactivated. Contact a User Admin.", 403)

    log_action(user, AuditLog.LOGIN, details="Mobile app", request=request)
    return JsonResponse(_user_json(user))


@csrf_exempt
@require_http_methods(["POST"])
def logout(request):
    user = User.objects.filter(pk=_body(request).get("userId") or 0).first()
    if user is None:
        return _error("Account not found", 404)
    log_action(user, AuditLog.LOGOUT, details="Mobile app", request=request)
    return JsonResponse({"message": "Logged out"})


@csrf_exempt
@require_http_methods(["POST"])
def reset_password(request):
    """Self-service "forgot password": identified by email + username."""
    data = _body(request)
    email = (data.get("email") or "").strip()
    username = (data.get("username") or "").strip()
    new_password = data.get("newPassword") or ""

    if not all([email, username, new_password]):
        return _error("Please fill in every field", 400)
    if len(new_password) < 6:
        return _error("Password must be at least 6 characters", 400)

    user = User.objects.filter(username=username, email__iexact=email).first()
    if user is None:
        return _error("No account matches that email and username", 404)

    user.set_password(new_password)
    user.save(update_fields=["password"])
    log_action(user, AuditLog.UPDATE, "Account", f"{username} reset their own password", request)
    return JsonResponse({"message": "Password reset. Please sign in."})


@csrf_exempt
@require_http_methods(["PUT"])
def update_my_profile(request, user_id):
    """Self-service profile edit: full name and email only."""
    data = _body(request)
    full_name = (data.get("fullName") or "").strip()
    email = (data.get("email") or "").strip()

    if not all([full_name, email]):
        return _error("Please fill in every field", 400)
    user = User.objects.filter(pk=user_id).first()
    if user is None:
        return _error("Account not found", 404)

    user.first_name, user.last_name = _split_name(full_name)
    user.email = email
    user.save(update_fields=["first_name", "last_name", "email"])
    log_action(user, AuditLog.UPDATE, "Account", f"{user.username} updated their own account details", request)
    return JsonResponse({"message": "Account updated", "fullName": user.get_full_name(), "email": email})


# ---------------------------------------------------------- collectibles --
# Starter catalog from the old Flask backend. Only served while researchers
# have not published any records yet, so a fresh install isn't empty.
STARTER_COLLECTIBLES = [
    {"id": "0042", "name": "Bandstand", "category": "Architecture", "rarity": "Common",
     "scientificName": None,
     "description": "A Victorian-era gazebo at the heart of the gardens, a favourite spot for weekend performances.",
     "feature": "Cast-iron columns, octagonal roof", "funFact": "Still hosts live performances today"},
    {"id": "0055", "name": "Vanda Miss Joaquim", "category": "Flora", "rarity": "Rare",
     "scientificName": "Papilionanthe Miss Joaquim",
     "description": "Singapore's national flower, a natural hybrid orchid first bred in a garden in 1893.",
     "feature": "Magenta petals, slender sepals", "funFact": "Singapore's national flower since 1981"},
    {"id": "0061", "name": "Vanda Tan Chay Yan", "category": "Flora", "rarity": "Common",
     "scientificName": "Vanda Tan Chay Yan",
     "description": "A hardy orchid hybrid named after one of Malaya's pioneering rubber planters.",
     "feature": "Deep purple blooms, waxy leaves", "funFact": "Named after Malaya's first rubber planter"},
    {"id": "0087", "name": "Tembusu Tree", "category": "Flora", "rarity": "Rare",
     "scientificName": "Cyrtophyllum fragrans",
     "description": "A heritage tree native to Southeast Asia, recognisable by its dark fissured bark.",
     "feature": "Dark fissured bark, cream flowers", "funFact": "Appears on the back of the Singapore $5 note"},
    {"id": "0093", "name": "Swan Lake", "category": "Architecture", "rarity": "Common",
     "scientificName": None,
     "description": "The gardens' oldest ornamental lake, home to a pair of black swans since the 1930s.",
     "feature": "Ornamental lake, resident black swans", "funFact": "The gardens' oldest water feature"},
    {"id": "0104", "name": "Rain Tree", "category": "Flora", "rarity": "Common",
     "scientificName": "Samanea saman",
     "description": "A broad-canopied shade tree whose leaflets fold up at dusk or before rain.",
     "feature": "Umbrella canopy, pink puff flowers", "funFact": "Leaflets fold up before rain"},
    {"id": "0118", "name": "Sundial Garden", "category": "Architecture", "rarity": "Rare",
     "scientificName": None,
     "description": "A heritage sundial marking the gardens' original 19th-century layout.",
     "feature": "Bronze gnomon, stone dial plate", "funFact": "Marks the gardens' original 19th-century layout"},
    {"id": "0131", "name": "Rafflesia", "category": "Flora", "rarity": "Rare",
     "scientificName": "Rafflesia arnoldii",
     "description": "One of the world's largest flowers, prized (and rarely seen in bloom) for its size and scent.",
     "feature": "Huge mottled petals, strong odour", "funFact": "One of the world's largest single flowers"},
]


def _visible_collectibles():
    """Published collectibles whose linked record is still verified, i.e. what visitors may see."""
    return [c for c in Collectible.objects.filter(status=Collectible.PUBLISHED)
            .select_related("category", "botanical_record", "architecture_record")
            if c.linked_available]


def _collectible_json(request, c, unlocked_at=None):
    """One collectible, shaped like the app's Collectible entity (Collectible.java)."""
    plant, place = c.botanical_record, c.architecture_record
    return {
        "id": str(c.pk),
        "name": c.name,
        "category": c.category.name,
        "rarity": c.get_rarity_display(),
        "scientificName": plant.scientific_name if plant else None,
        "description": c.description,
        "feature": plant.family if plant else f"{place.architectural_style}, {place.construction_date}",
        "funFact": (plant or place).fun_fact,
        "imageUrl": request.build_absolute_uri(f"/api/collectibles/{c.pk}/image") if c.image else "",
        "discovered": unlocked_at is not None,
        "unlockedAt": unlocked_at.isoformat() if unlocked_at else None,
    }


def _visitor(request):
    """The logged-in app user named by ?userId= / the userId form field, or None."""
    user_id = request.GET.get("userId") or request.POST.get("userId") or ""
    return User.objects.filter(pk=user_id).first() if user_id.isdigit() else None


@require_http_methods(["GET"])
def collectibles(request):
    """The collectible catalog. With ?userId=, each card also says whether that visitor
    has discovered it, and when (from CollectibleUnlock)."""
    cards = _visible_collectibles()
    if not cards:
        return JsonResponse({"collectibles": STARTER_COLLECTIBLES})
    user = _visitor(request)
    unlocks = {} if user is None else dict(
        CollectibleUnlock.objects.filter(visitor_username=user.username)
        .values_list("collectible_id", "unlocked_at"))
    return JsonResponse({"collectibles": [_collectible_json(request, c, unlocks.get(c.pk)) for c in cards]})


@require_http_methods(["GET"])
def collectible_image(request, pk):
    """A published collectible's card picture. The app is not logged in to the portal,
    so it can't use /files/ (staff only); this serves only what visitors may see."""
    card = next((c for c in _visible_collectibles() if c.pk == pk), None)
    stored = StoredFile.objects.filter(name=card.image.name).first() if card and card.image else None
    if stored is None:
        raise Http404("No such picture")
    response = HttpResponse(bytes(stored.content), content_type=stored.content_type or "image/jpeg")
    response["Cache-Control"] = "public, max-age=3600"
    response["X-Content-Type-Options"] = "nosniff"
    return response


def _unlock_for_scan(request, label):
    """If a published collectible uses this model label, record that the visitor unlocked it.
    Returns (collectible JSON, newly unlocked) or (None, False)."""
    card = next((c for c in _visible_collectibles() if c.model_label == label), None)
    if card is None:
        return None, False
    user = _visitor(request)
    if user is None:
        return _collectible_json(request, card), False
    unlock, created = CollectibleUnlock.objects.get_or_create(collectible=card, visitor_username=user.username)
    return _collectible_json(request, card, unlock.unlocked_at), created


# --------------------------------------------------------------- identify --
@csrf_exempt
@require_http_methods(["POST"])
def identify(request):
    """Identifies the photo with our own PlantDex model (classifier.py) when it is
    confident enough; otherwise forwards the photo to Pl@ntNet and returns its JSON
    unchanged. Both replies have the same shape: results[0].species / .score.
    The "source" field says which one answered ("plantdex" or "plantnet")."""
    photo = request.FILES.get("image")
    if photo is None:
        return JsonResponse({"error": "No image uploaded."}, status=400)
    if photo.content_type not in ("image/jpeg", "image/png"):
        return JsonResponse({"error": "Upload a JPEG or PNG image."}, status=415)

    model_guess = None
    if classifier.available():
        try:
            label, score = classifier.classify(photo)[0]
            model_guess = classifier.as_plantnet_result(label, score)
        except Exception:
            logger.exception("PlantDex model failed; falling back to Pl@ntNet")
        photo.seek(0)
    if model_guess and model_guess["score"] >= MIN_MODEL_CONFIDENCE:
        card, newly_unlocked = _unlock_for_scan(request, model_guess["label"])
        return JsonResponse({"source": "plantdex", "results": [model_guess],
                             "collectible": card, "newlyUnlocked": newly_unlocked})

    api_key = os.environ.get("PLANTNET_API_KEY")
    if not api_key:
        if model_guess:   # model unsure and no Pl@ntNet to ask: treat as no match
            return JsonResponse({"source": "plantdex", "matched": False, "results": []})
        return JsonResponse({"error": "Pl@ntNet API key is missing."}, status=500)

    try:
        response = requests.post(
            "https://my-api.plantnet.org/v2/identify/all",
            params={"api-key": api_key, "include-related-images": "true", "nb-results": 1},
            files={"images": (photo.name, photo.file, photo.content_type)},
            timeout=30,
        )
    except requests.RequestException:
        return JsonResponse({"error": "Could not reach Pl@ntNet."}, status=502)

    if response.status_code == 404:
        return JsonResponse({"matched": False, "results": []})
    if not response.ok:
        return JsonResponse({"error": "Pl@ntNet returned an error.", "status": response.status_code}, status=502)
    try:
        data = response.json()
        if isinstance(data, dict):
            data["source"] = "plantnet"
        return JsonResponse(data, safe=False)
    except ValueError:
        return JsonResponse({"error": "Invalid response from Pl@ntNet."}, status=502)


# No trailing slashes: these are the exact paths ApiClient.java calls.
urls = [
    path("api/auth/register", register, name="api_register"),
    path("api/auth/login", login, name="api_login"),
    path("api/auth/logout", logout, name="api_logout"),
    path("api/auth/reset-password", reset_password, name="api_reset_password"),
    path("api/users/<int:user_id>", update_my_profile, name="api_update_profile"),
    path("api/collectibles", collectibles, name="api_collectibles"),
    path("api/collectibles/<int:pk>/image", collectible_image, name="api_collectible_image"),
    path("api/identify", identify, name="api_identify"),
]
