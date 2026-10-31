from django.core.paginator import Paginator
from django.shortcuts import get_object_or_404, render

from .decorators import feature_required
from .models import CollectibleErrorLog
from .roles import get_role
from .views import _safe_date


@feature_required("error-logs")
def error_log_list(request):                                 # SA-09
    logs = CollectibleErrorLog.objects.all()

    error_type = request.GET.get("type", "")
    status = request.GET.get("status", "")
    collectible = request.GET.get("collectible", "").strip()
    user_q = request.GET.get("user", "").strip()
    date_from = _safe_date(request.GET.get("from"))
    date_to = _safe_date(request.GET.get("to"))

    if error_type:
        logs = logs.filter(error_type=error_type)
    if status:
        logs = logs.filter(status=status)
    if collectible:
        logs = logs.filter(collectible_name__icontains=collectible)
    if user_q:
        logs = logs.filter(visitor_username__icontains=user_q)
    if date_from:
        logs = logs.filter(occurred_at__date__gte=date_from)
    if date_to:
        logs = logs.filter(occurred_at__date__lte=date_to)

    page = Paginator(logs, 15).get_page(request.GET.get("page"))

    params = request.GET.copy()
    params.pop("page", None)

    return render(request, "portal/error_log_list.html", {
        "role": get_role(request.user),
        "page": page,
        "error_types": CollectibleErrorLog.ERROR_TYPES,
        "statuses": CollectibleErrorLog.STATUSES,
        "filters": {"type": error_type, "status": status, "collectible": collectible,
                    "user": user_q, "from": request.GET.get("from", ""),
                    "to": request.GET.get("to", "")},
        "querystring": params.urlencode(),
    })


@feature_required("error-logs")
def error_log_detail(request, pk):                           # SA-09
    log = get_object_or_404(CollectibleErrorLog, pk=pk)
    return render(request, "portal/error_log_detail.html",
                  {"role": get_role(request.user), "log": log})
