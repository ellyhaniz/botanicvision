from django.contrib import messages
from django.contrib.auth.models import User
from django.core.paginator import Paginator
from django.db import transaction
from django.db.models import Q
from django.shortcuts import get_object_or_404, redirect, render

from .audit import log_action
from .decorators import feature_required
from .forms import AccountForm
from .models import AuditLog
from .roles import ROLES, get_role



def _role_name(user):
    """Role of any user. Works with prefetched groups, so the list stays fast."""
    if user.is_superuser:
        return "System Admin"
    for g in user.groups.all():
        if g.name in ROLES:
            return g.name
    return None


def _ctx(request, **extra):
    return {"role": get_role(request.user), **extra}


@feature_required("accounts")
def account_list(request):                                   # UA-07 + UA-09
    q = request.GET.get("q", "").strip()
    status = request.GET.get("status", "")

    users = User.objects.prefetch_related("groups").order_by("username")
    if q:
        users = users.filter(
            Q(username__icontains=q) | Q(first_name__icontains=q)
            | Q(last_name__icontains=q) | Q(email__icontains=q)
        )
    if status == "active":
        users = users.filter(is_active=True)
    elif status == "suspended":
        users = users.filter(is_active=False)

    page = Paginator(users, 10).get_page(request.GET.get("page"))
    page.object_list = list(page.object_list)
    for u in page.object_list:
        u.role_name = _role_name(u)

    params = request.GET.copy()
    params.pop("page", None)
    return render(request, "portal/account_list.html", _ctx(
        request, page=page, q=q, status=status, querystring=params.urlencode()))


@feature_required("accounts")
def account_detail(request, pk):                             # UA-07
    target = get_object_or_404(User, pk=pk)
    return render(request, "portal/account_detail.html", _ctx(
        request, target=target, target_role=_role_name(target),
        is_self=(target == request.user)))


@feature_required("accounts")
def account_edit(request, pk):                               # UA-08
    target = get_object_or_404(User, pk=pk)
    is_self = target == request.user
    old_role = _role_name(target)

    form = AccountForm(request.POST or None, instance=target,
                       lock_profile=is_self, current_role=old_role)

    if request.method == "POST" and form.is_valid():
        changed = form.changed_data
        if not changed:
            messages.info(request, "No changes to save.")
            return redirect("account_detail", pk=target.pk)

        parts = []
        with transaction.atomic():
            form.save()
            for field in changed:                
                if field == "profile_type":
                    new_group = form.cleaned_data["profile_type"]
                    target.groups.remove(*target.groups.filter(name__in=ROLES))
                    if new_group:
                        target.groups.add(new_group)
                    parts.append(f"profile type {old_role or 'Visitor'} → "
                                 f"{new_group.name if new_group else 'Visitor'}")
                else:
                    parts.append(field.replace("_", " "))
            log_action(request.user, AuditLog.UPDATE, record_type="User Account",
                       details=f"{target.username}: {', '.join(parts)}", request=request)

        messages.success(request, f"Account '{target.username}' was updated.")
        return redirect("account_detail", pk=target.pk)

    return render(request, "portal/account_form.html",
                  _ctx(request, form=form, target=target, is_self=is_self))


@feature_required("accounts")
def account_status(request, pk):                             # UA-10
    target = get_object_or_404(User, pk=pk)

    if target == request.user:                               # own-account protection
        messages.error(request, "You cannot change the status of your own account.")
        return redirect("account_detail", pk=pk)

    if request.method == "POST":
        target.is_active = not target.is_active
        target.save(update_fields=["is_active"])
        verb = "Reactivated" if target.is_active else "Suspended"
        log_action(request.user, AuditLog.UPDATE, record_type="User Account",
                   details=f"{verb} {target.username}", request=request)
        messages.success(request, f"Account '{target.username}' was {verb.lower()}.")
        return redirect("account_detail", pk=pk)

    return render(request, "portal/account_confirm_status.html",
                  _ctx(request, target=target))