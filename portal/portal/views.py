from django.contrib.auth.decorators import login_required
from django.core.exceptions import PermissionDenied
from django.http import Http404
from django.shortcuts import render
from django.core.paginator import Paginator
from django.utils.dateparse import parse_date
from django.contrib.auth.views import PasswordResetConfirmView
from .audit import log_action
from .models import AuditLog
from .roles import SECTIONS, allowed_slugs, get_role

@login_required
def dashboard(request):
    role = get_role(request.user)
    allowed = allowed_slugs(request.user)
    cards = [{"slug": s, **d} for s, d in SECTIONS.items() if s in allowed]
    return render(request, "portal/dashboard.html", {"role": role, "cards": cards})

@login_required
def section(request, slug):
    data = SECTIONS.get(slug)
    if data is None:
        raise Http404
    if slug not in allowed_slugs(request.user):
        raise PermissionDenied
    return render(request, "portal/section.html",
                  {"role": get_role(request.user), "section": data})

def _safe_date(value):
    try:
        return parse_date(value) if value else None
    except ValueError:
        return None


@login_required
def audit_logs(request):
    role = get_role(request.user)
    if "audit-logs" not in allowed_slugs(request.user):
        raise PermissionDenied

    logs = AuditLog.objects.all()

    user_q = request.GET.get("user", "").strip()
    action = request.GET.get("action", "")
    record_type = request.GET.get("record_type", "")
    date_from = _safe_date(request.GET.get("from"))
    date_to = _safe_date(request.GET.get("to"))

    if user_q:
        logs = logs.filter(username__icontains=user_q)
    if action:
        logs = logs.filter(action=action)
    if record_type:
        logs = logs.filter(record_type=record_type)
    if date_from:
        logs = logs.filter(timestamp__date__gte=date_from)
    if date_to:
        logs = logs.filter(timestamp__date__lte=date_to)

    # The dropdown lists only record types that actually appear in the log
    record_types = (AuditLog.objects.exclude(record_type="")
                    .order_by("record_type")
                    .values_list("record_type", flat=True).distinct())

    page = Paginator(logs, 15).get_page(request.GET.get("page"))

    params = request.GET.copy()
    params.pop("page", None)

    return render(request, "portal/audit_logs.html", {
        "role": role,
        "page": page,
        "actions": AuditLog.ACTIONS,
        "record_types": record_types,
        "filters": {"user": user_q, "action": action, "record_type": record_type,
                    "from": request.GET.get("from", ""), "to": request.GET.get("to", "")},
        "querystring": params.urlencode(),
    })

class PortalPasswordResetConfirmView(PasswordResetConfirmView):
    template_name = "portal/password_reset_confirm.html"

    def form_valid(self, form):
        response = super().form_valid(form)          # saves the new password
        log_action(self.user, AuditLog.UPDATE, record_type="User Account",
                   details=f"{self.user.get_username()}: password reset via email link",
                   request=self.request)
        return response
