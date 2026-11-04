"""
System Admin: discovery collectibles (SA-04 .. SA-08).

    SA-04 create   SA-05 view / list   SA-06 update   SA-07 delete   SA-08 search + filter

Pictures are stored in the database (see storage.py), so they are shared by everyone who
uses the same Azure SQL database.
"""
from django.contrib import messages
from django.core.paginator import Paginator
from django.db import transaction
from django.db.models import Count, Q, ProtectedError
from django.shortcuts import redirect, render

from .audit import log_action
from .collectible_forms import CollectibleForm
from .decorators import feature_required
from .models import AuditLog, Collectible, CollectibleCategory
from .roles import get_role

PAGE_SIZE = 10
RECORD_TYPE = "Collectible"          # label written to the audit log
gate = feature_required("collectibles")


def _ctx(request, **extra):
    return {"role": get_role(request.user), **extra}


def _find(request, pk):
    """The collectible, or None (with a message) if it was deleted in the meantime."""
    try:
        return (Collectible.objects.select_related("category", "botanical_record", "architecture_record")
                .get(pk=pk))
    except Collectible.DoesNotExist:
        messages.error(request, "That collectible no longer exists. It may have been removed.")
        return None


def _in_use_message(collectible, unlocks):
    return (f"'{collectible.name}' has been unlocked by {unlocks} visitor{'s' if unlocks != 1 else ''}, "
            "so it cannot be deleted. Set its status to Draft to hide it from visitors instead.")


# ---- list + search + filter (SA-05 / SA-08) -----------------------------------
@gate
def collectible_list(request):
    q = request.GET.get("q", "").strip()
    category = request.GET.get("category", "")
    rarity = request.GET.get("rarity", "")
    status = request.GET.get("status", "")
    kind = request.GET.get("kind", "")

    items = (Collectible.objects
             .select_related("category", "botanical_record", "architecture_record")
             .annotate(unlock_count=Count("unlocks", distinct=True))
             # Explicit order: a GROUP BY query ignores the model's default ordering, and
             # paging (especially on SQL Server) needs a stable order to avoid shuffling pages.
             .order_by("name", "pk"))
    if q:
        items = items.filter(
            Q(name__icontains=q) | Q(description__icontains=q)
            | Q(botanical_record__common_name__icontains=q)
            | Q(botanical_record__scientific_name__icontains=q)
            | Q(architecture_record__name__icontains=q))
    if category.isdigit():
        items = items.filter(category_id=int(category))
    if rarity in dict(Collectible.RARITIES):
        items = items.filter(rarity=rarity)
    if status in dict(Collectible.STATUSES):
        items = items.filter(status=status)
    if kind == "botanical":
        items = items.filter(botanical_record__isnull=False)
    elif kind == "architecture":
        items = items.filter(architecture_record__isnull=False)

    page = Paginator(items, PAGE_SIZE).get_page(request.GET.get("page"))
    params = request.GET.copy()
    params.pop("page", None)
    return render(request, "portal/collectible_list.html", _ctx(
        request, page=page, q=q, category=category, rarity=rarity, status=status, kind=kind,
        categories=CollectibleCategory.objects.all(), rarities=Collectible.RARITIES,
        statuses=Collectible.STATUSES, querystring=params.urlencode(),
        filtered=any([q, category, rarity, status, kind])))


# ---- create (SA-04) -----------------------------------------------------------
@gate
def collectible_create(request):
    form = CollectibleForm(request.POST, request.FILES) if request.method == "POST" else CollectibleForm()
    if request.method == "POST" and form.is_valid():
        with transaction.atomic():
            obj = form.save(commit=False)
            obj.created_by = request.user
            obj.save()
            log_action(request.user, AuditLog.CREATE, record_type=RECORD_TYPE,
                       details=f"{obj.name} (linked to {obj.linked_kind.lower()} record '{obj.linked_name}')",
                       request=request)
        messages.success(request, f"Collectible '{obj.name}' was created.")
        return redirect("collectible_detail", pk=obj.pk)
    return render(request, "portal/collectible_form.html", _ctx(request, form=form, collectible=None))


# ---- view (SA-05) -------------------------------------------------------------
@gate
def collectible_detail(request, pk):
    obj = _find(request, pk)
    if obj is None:
        return redirect("collectible_list")
    return render(request, "portal/collectible_detail.html", _ctx(
        request, collectible=obj, unlock_count=obj.unlocks.count(),
        recent_unlocks=obj.unlocks.all()[:5]))


# ---- update (SA-06) -----------------------------------------------------------
@gate
def collectible_update(request, pk):
    obj = _find(request, pk)
    if obj is None:
        return redirect("collectible_list")
    old_name, old_image = obj.name, obj.image.name
    form = (CollectibleForm(request.POST, request.FILES, instance=obj)
            if request.method == "POST" else CollectibleForm(instance=obj))

    if request.method == "POST" and form.is_valid():
        if not form.has_changed():
            messages.info(request, "No changes to save.")
            return redirect("collectible_detail", pk=obj.pk)
        changed = [form.fields[f].label or f.replace("_", " ") for f in form.changed_data]
        with transaction.atomic():
            form.save()
            log_action(request.user, AuditLog.UPDATE, record_type=RECORD_TYPE,
                       details=f"{old_name}: {', '.join(changed)}", request=request)
        if obj.image.name != old_image:                    # do not leave the old picture behind
            obj.image.storage.delete(old_image)
        messages.success(request, f"Collectible '{obj.name}' was updated.")
        return redirect("collectible_detail", pk=obj.pk)

    return render(request, "portal/collectible_form.html", _ctx(
        request, form=form, collectible=obj, unlock_count=obj.unlocks.count()))


# ---- delete (SA-07) -----------------------------------------------------------
@gate
def collectible_delete(request, pk):
    obj = _find(request, pk)
    if obj is None:
        return redirect("collectible_list")

    unlocks = obj.unlocks.count()
    if unlocks:                                            # "Collectible in use" alternative flow
        messages.error(request, _in_use_message(obj, unlocks))
        return redirect("collectible_detail", pk=obj.pk)

    if request.method == "POST":
        name, image = obj.name, obj.image.name
        try:
            with transaction.atomic():
                obj.delete()
                log_action(request.user, AuditLog.DELETE, record_type=RECORD_TYPE,
                           details=name, request=request)
        except ProtectedError:                             # someone unlocked it a moment ago
            messages.error(request, _in_use_message(obj, obj.unlocks.count()))
            return redirect("collectible_detail", pk=obj.pk)
        obj.image.storage.delete(image)
        messages.success(request, f"Collectible '{name}' was deleted.")
        return redirect("collectible_list")

    return render(request, "portal/collectible_confirm_delete.html", _ctx(request, collectible=obj))
