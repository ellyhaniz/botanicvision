from django.contrib import messages
from django.db import transaction
from django.db.models import Count, Q
from django.shortcuts import get_object_or_404, redirect, render

from .audit import log_action
from .decorators import feature_required
from .forms import ProfileTypeForm
from .models import AuditLog, ProfileType
from .roles import get_role


def _ctx(request, **extra):
    return {"role": get_role(request.user), **extra}


@feature_required("profile-types")
def profile_list(request):                                   # UA-04 + UA-06
    q = request.GET.get("q", "").strip()
    items = (ProfileType.objects.select_related("group")
             .annotate(user_count=Count("group__user", distinct=True),
                       perm_count=Count("permissions", distinct=True)))
    if q:
        items = items.filter(Q(group__name__icontains=q) | Q(description__icontains=q))
    return render(request, "portal/profile_list.html", _ctx(request, items=items, q=q))


@feature_required("profile-types")
def profile_detail(request, pk):                             # UA-04
    pt = get_object_or_404(ProfileType.objects.select_related("group"), pk=pk)
    return render(request, "portal/profile_detail.html", _ctx(
        request, pt=pt, perms=pt.permissions.all(),
        user_count=pt.group.user_set.count()))


@feature_required("profile-types")
def profile_edit(request, pk):                               # UA-05
    pt = get_object_or_404(ProfileType.objects.select_related("group"), pk=pk)
    old_perms = set(pt.permissions.values_list("title", flat=True))
    form = ProfileTypeForm(request.POST or None, instance=pt)

    if request.method == "POST" and form.is_valid():
        if not form.has_changed():
            messages.info(request, "No changes to save.")
            return redirect("profile_detail", pk=pt.pk)

        parts = []
        with transaction.atomic():
            form.save()
            if "description" in form.changed_data:
                parts.append("description")
            if "permissions" in form.changed_data:
                new_perms = set(pt.permissions.values_list("title", flat=True))
                if new_perms - old_perms:
                    parts.append("added " + ", ".join(sorted(new_perms - old_perms)))
                if old_perms - new_perms:
                    parts.append("removed " + ", ".join(sorted(old_perms - new_perms)))
            log_action(request.user, AuditLog.UPDATE, record_type="Profile Type",
                       details=f"{pt.group.name}: {'; '.join(parts)}", request=request)

        messages.success(request, f"Profile type '{pt.group.name}' was updated.")
        return redirect("profile_detail", pk=pt.pk)

    return render(request, "portal/profile_form.html", _ctx(request, form=form, pt=pt))