"""
Create / view / update / delete / search pages for Researcher records.

Botanical and architecture records follow the same five stories, so they share one
set of views. Each record type is described by a RecordConfig at the bottom.
"""
from dataclasses import dataclass
from types import SimpleNamespace

from django.contrib import messages
from django.core.paginator import Paginator
from django.db import transaction
from django.db.models import Q
from django.http import JsonResponse
from django.shortcuts import redirect, render
from django.urls import path, reverse

from .audit import log_action
from .decorators import feature_required
from .forms import ArchitectureRecordForm, BotanicalRecordForm
from .models import ArchitectureRecord, AuditLog, BotanicalRecord
from .roles import get_role

PAGE_SIZE = 10


@dataclass
class RecordConfig:
    key: str                    # feature slug AND url prefix, e.g. "botanical"
    model: type
    form_class: type
    record_type: str            # label written to the audit log
    title: str                  # page heading
    blurb: str                  # sub-heading
    singular: str               # "botanical record" (used in messages)
    name_attr: str              # attribute that names a record
    search_fields: list         # fields matched by the search box
    search_placeholder: str
    columns: list               # [(heading, attribute)] shown in the list
    detail_fields: list         # [(label, attribute)] shown on the detail page
    duplicate_field: str        # field checked for "this may already exist"
    duplicate_label: str        # how that field is called in the warning

    def url(self, suffix):
        return f"{self.key}_{suffix}"

    # Used by the templates, e.g. {% url cfg.url_list %}
    url_list = property(lambda self: self.url("list"))
    url_new = property(lambda self: self.url("new"))
    url_detail = property(lambda self: self.url("detail"))
    url_edit = property(lambda self: self.url("edit"))
    url_delete = property(lambda self: self.url("delete"))
    url_check = property(lambda self: self.url("check"))


def bound_form(request, form_class, **kwargs):
    """The form filled with the submitted data on POST (even an empty POST), else blank."""
    if request.method == "POST":
        return form_class(request.POST, request.FILES, **kwargs)
    return form_class(**kwargs)


def make_views(cfg):
    """Build the views and URL patterns for one record type."""
    gate = feature_required(cfg.key)      # server-side access control

    def ctx(request, **extra):
        return {"role": get_role(request.user), "cfg": cfg, **extra}

    def name_of(obj):
        return getattr(obj, cfg.name_attr)

    def find(request, pk):
        """The record, or None (with a message) if it was deleted in the meantime."""
        try:
            return cfg.model.objects.get(pk=pk)
        except cfg.model.DoesNotExist:
            messages.error(request, f"That {cfg.singular} no longer exists. It may have been removed.")
            return None

    # ---- list + search (RS-08 / RS-13) -------------------------------------
    @gate
    def record_list(request):
        q = request.GET.get("q", "").strip()
        records = cfg.model.objects.all()
        if q:
            query = Q()
            for field in cfg.search_fields:
                query |= Q(**{f"{field}__icontains": q})
            records = records.filter(query)

        page = Paginator(records, PAGE_SIZE).get_page(request.GET.get("page"))
        rows = [{"obj": obj, "cells": [getattr(obj, attr) for _, attr in cfg.columns]}
                for obj in page.object_list]
        params = request.GET.copy()
        params.pop("page", None)
        return render(request, "portal/record_list.html", ctx(
            request, page=page, rows=rows, q=q, querystring=params.urlencode()))

    # ---- create (RS-04 / RS-09) --------------------------------------------
    @gate
    def record_create(request):
        form = bound_form(request, cfg.form_class)
        if request.method == "POST" and form.is_valid():
            with transaction.atomic():
                obj = form.save(commit=False)
                obj.created_by = request.user
                obj.save()
                log_action(request.user, AuditLog.CREATE, record_type=cfg.record_type,
                           details=name_of(obj), request=request)
            messages.success(request, f"{cfg.singular.capitalize()} '{name_of(obj)}' was created.")
            return redirect(cfg.url_detail, pk=obj.pk)
        return render(request, "portal/record_form.html", ctx(request, form=form, record=None))

    # ---- view (RS-05 / RS-10) ----------------------------------------------
    @gate
    def record_detail(request, pk):
        obj = find(request, pk)
        if obj is None:
            return redirect(cfg.url_list)
        fields = [(label, getattr(obj, attr)) for label, attr in cfg.detail_fields]
        return render(request, "portal/record_detail.html", ctx(
            request, record=obj, fields=fields, name=name_of(obj)))

    # ---- update (RS-06 / RS-11) --------------------------------------------
    @gate
    def record_update(request, pk):
        obj = find(request, pk)
        if obj is None:
            return redirect(cfg.url_list)
        old_name = name_of(obj)
        form = bound_form(request, cfg.form_class, instance=obj)

        if request.method == "POST" and form.is_valid():
            if not form.has_changed():
                messages.info(request, "No changes to save.")
                return redirect(cfg.url_detail, pk=obj.pk)
            changed = [form.fields[f].label or f.replace("_", " ") for f in form.changed_data]
            with transaction.atomic():
                form.save()
                log_action(request.user, AuditLog.UPDATE, record_type=cfg.record_type,
                           details=f"{old_name}: {', '.join(changed)}", request=request)
            messages.success(request, f"{cfg.singular.capitalize()} '{name_of(obj)}' was updated.")
            return redirect(cfg.url_detail, pk=obj.pk)

        return render(request, "portal/record_form.html", ctx(request, form=form, record=obj))

    # ---- delete (RS-07 / RS-12) --------------------------------------------
    @gate
    def record_delete(request, pk):
        obj = find(request, pk)
        if obj is None:
            return redirect(cfg.url_list)
        if request.method == "POST":
            name = name_of(obj)
            with transaction.atomic():
                obj.soft_delete()
                log_action(request.user, AuditLog.DELETE, record_type=cfg.record_type,
                           details=name, request=request)
            messages.success(request, f"{cfg.singular.capitalize()} '{name}' was deleted.")
            return redirect(cfg.url_list)
        return render(request, "portal/record_confirm_delete.html",
                      ctx(request, record=obj, name=name_of(obj)))

    # ---- duplicate warning (called by the form page while typing) ----------
    @gate
    def record_check_duplicate(request):
        value = " ".join(request.GET.get("value", "").split())
        match = None
        if value:
            matches = cfg.model.objects.filter(**{f"{cfg.duplicate_field}__iexact": value})
            exclude = request.GET.get("exclude", "")
            if exclude.isdigit():                      # editing: ignore the record itself
                matches = matches.exclude(pk=int(exclude))
            match = matches.first()
        if match is None:
            return JsonResponse({"duplicate": False})
        return JsonResponse({"duplicate": True, "name": name_of(match),
                             "url": reverse(cfg.url_detail, args=[match.pk])})

    k = cfg.key
    urls = [
        path(f"section/{k}/", record_list, name=cfg.url("list")),
        path(f"section/{k}/new/", record_create, name=cfg.url("new")),
        path(f"section/{k}/check-duplicate/", record_check_duplicate, name=cfg.url("check")),
        path(f"section/{k}/<int:pk>/", record_detail, name=cfg.url("detail")),
        path(f"section/{k}/<int:pk>/edit/", record_update, name=cfg.url("edit")),
        path(f"section/{k}/<int:pk>/delete/", record_delete, name=cfg.url("delete")),
    ]
    return SimpleNamespace(cfg=cfg, urls=urls)


# ---------------------------------------------------------------------------
# The two record types
# ---------------------------------------------------------------------------
botanical = make_views(RecordConfig(
    key="botanical",
    model=BotanicalRecord,
    form_class=BotanicalRecordForm,
    record_type="Botanical Record",
    title="Botanical Records",
    blurb="Plants and flowers that visitors can identify and collect.",
    singular="botanical record",
    name_attr="common_name",
    search_fields=["common_name", "scientific_name", "family"],
    search_placeholder="Common name, scientific name or family",
    columns=[("Common name", "common_name"), ("Scientific name", "scientific_name"),
             ("Family", "family")],
    detail_fields=[("Common name", "common_name"), ("Scientific name", "scientific_name"),
                   ("Family", "family"),
                   ("Identification features", "identification_features"),
                   ("Fun fact", "fun_fact")],
    duplicate_field="scientific_name",
    duplicate_label="scientific name",
))

architecture = make_views(RecordConfig(
    key="architecture",
    model=ArchitectureRecord,
    form_class=ArchitectureRecordForm,
    record_type="Architecture Record",
    title="Architecture Records",
    blurb="Heritage structures that visitors can identify and collect.",
    singular="architecture record",
    name_attr="name",
    search_fields=["name", "location", "architectural_style", "description"],
    search_placeholder="Structure name, location or keyword",
    columns=[("Structure", "name"), ("Location", "location"),
             ("Style", "architectural_style"), ("Built", "construction_date")],
    detail_fields=[("Structure name", "name"), ("Location", "location"),
                   ("Architectural style", "architectural_style"),
                   ("Construction date", "construction_date"),
                   ("Description", "description"), ("Fun fact", "fun_fact")],
    duplicate_field="name",
    duplicate_label="structure name",
))