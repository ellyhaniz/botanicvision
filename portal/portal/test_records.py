"""
Tests for the Researcher record pages (RS-04 .. RS-13).

One test mixin is run twice: once for botanical records, once for architecture
records, because both are built from the same code (see portal/records.py).
"""
import io
import shutil
import tempfile
import unittest
from unittest import mock

from django.core.files.uploadedfile import SimpleUploadedFile
from django.core.management import call_command
from django.test import override_settings
from django.urls import reverse
from PIL import Image

from .models import ArchitectureRecord, AuditLog, BotanicalRecord, ProfileType
from .tests import PortalTestCase, client_for

TEMP_MEDIA = tempfile.mkdtemp(prefix="plantdex-test-media-")   # uploads during tests go here
unittest.addModuleCleanup(shutil.rmtree, TEMP_MEDIA, True)


def png(name="leaf.png", size=(12, 12), colour=(40, 140, 80)):
    """A small but genuinely valid image file."""
    buf = io.BytesIO()
    Image.new("RGB", size, colour).save(buf, "PNG")
    return SimpleUploadedFile(name, buf.getvalue(), content_type="image/png")


class RecordCrudMixin:
    """Subclasses describe one record type. This class itself is not run."""
    key = None              # "botanical" / "architecture"
    model = None
    record_type = None
    name_field = None
    search_field = None     # a field the search box must match
    duplicate_field = None

    # --- subclass hooks ---------------------------------------------------
    def valid_data(self, **overrides):
        raise NotImplementedError

    # --- helpers ----------------------------------------------------------
    def url(self, suffix, *args):
        return reverse(f"{self.key}_{suffix}", args=args)

    def create(self, **overrides):
        return self.client.post(self.url("new"), self.valid_data(**overrides))

    def make(self, **overrides):
        """Create a record straight in the database (no HTTP)."""
        data = self.valid_data(**overrides)
        data.pop("image")
        obj = self.model(**data)
        obj.image.save("x.png", png(), save=False)
        obj.save()
        return obj

    def current_values(self, obj):
        """The form data that would re-submit the record unchanged (no new image)."""
        return {k: getattr(obj, k) for k in self.valid_data() if k != "image"}

    def setUp(self):
        super().setUp()
        self.login("researcher")

    # --- access control ---------------------------------------------------
    def test_only_researchers_can_open_the_pages(self):
        obj = self.make()
        pages = [self.url("list"), self.url("new"), self.url("detail", obj.pk),
                 self.url("edit", obj.pk), self.url("delete", obj.pk), self.url("check")]
        for url in pages:
            self.assertEqual(self.client.get(url).status_code, 200, url)
            for other in ("useradmin", "sysadmin", "visitor01"):
                self.assertEqual(client_for(other).get(url).status_code, 403, f"{other} {url}")
            self.client.logout()
            r = self.client.get(url)
            self.assertEqual(r.status_code, 302, url)
            self.assertIn("/login/", r["Location"])
            self.login("researcher")

    def test_other_roles_cannot_post_either(self):
        obj = self.make()
        before = self.model.objects.count()
        for other in ("useradmin", "sysadmin"):
            c = client_for(other)
            self.assertEqual(c.post(self.url("new"), self.valid_data()).status_code, 403)
            self.assertEqual(c.post(self.url("delete", obj.pk)).status_code, 403)
        self.assertEqual(self.model.objects.count(), before)
        self.assertFalse(self.model.all_objects.get(pk=obj.pk).is_deleted)

    def test_permission_comes_from_the_profile_type(self):
        """Taking the permission away from the Researcher profile closes the page."""
        pt = ProfileType.objects.get(group__name="Researcher")
        pt.permissions.remove(*pt.permissions.filter(code=self.key))
        self.assertEqual(self.client.get(self.url("list")).status_code, 403)

    def test_dashboard_card_opens_the_real_page(self):
        r = self.client.get(f"/section/{self.key}/")
        self.assertNotContains(r, "under construction")
        self.assertEqual(r.resolver_match.url_name, f"{self.key}_list")

    # --- create -----------------------------------------------------------
    def test_create_saves_record_image_and_audit_entry(self):
        r = self.create()
        obj = self.model.objects.get()
        self.assertRedirects(r, self.url("detail", obj.pk))
        self.assertTrue(obj.image.name.startswith(f"{self.key}/"))
        self.assertEqual(obj.created_by, self.researcher)
        log = AuditLog.objects.get(action=AuditLog.CREATE)
        self.assertEqual(log.record_type, self.record_type)
        self.assertEqual(log.details, getattr(obj, self.name_field))
        self.assertEqual(log.username, "researcher")

    def test_create_rejects_missing_fields_and_writes_nothing(self):
        r = self.client.post(self.url("new"), {})
        self.assertEqual(r.status_code, 200)
        self.assertEqual(self.model.objects.count(), 0)
        self.assertFalse(AuditLog.objects.filter(action=AuditLog.CREATE).exists())
        required = [n for n, f in r.context["form"].fields.items() if f.required]
        self.assertTrue(set(required) <= set(r.context["form"].errors), required)

    def test_create_requires_an_image(self):
        data = self.valid_data()
        del data["image"]
        r = self.client.post(self.url("new"), data)
        self.assertEqual(r.status_code, 200)
        self.assertIn("image", r.context["form"].errors)
        self.assertEqual(self.model.objects.count(), 0)

    def test_rejects_wrong_file_type(self):
        bad = SimpleUploadedFile("notes.txt", b"not an image", content_type="text/plain")
        r = self.client.post(self.url("new"), self.valid_data(image=bad))
        self.assertEqual(r.status_code, 200)
        self.assertIn("image", r.context["form"].errors)
        self.assertEqual(self.model.objects.count(), 0)

    def test_rejects_wrong_extension_even_if_content_is_an_image(self):
        r = self.client.post(self.url("new"), self.valid_data(image=png(name="photo.gif")))
        self.assertIn("JPG, PNG or WebP", str(r.context["form"].errors["image"]))

    def test_rejects_oversized_image(self):
        with mock.patch("portal.forms.MAX_IMAGE_MB", 0.00001):
            r = self.client.post(self.url("new"), self.valid_data())
        self.assertIn("smaller", str(r.context["form"].errors["image"]))
        self.assertEqual(self.model.objects.count(), 0)

    def test_text_is_tidied(self):
        self.create(**{self.name_field: "  Spaced    out   name  "})
        self.assertEqual(getattr(self.model.objects.get(), self.name_field), "Spaced out name")

    # --- duplicate warning (a warning, never a block) ---------------------
    def test_duplicates_are_allowed_but_reported(self):
        first = self.make()
        value = getattr(first, self.duplicate_field)
        check = self.client.get(self.url("check"), {"value": value.upper()}).json()
        self.assertTrue(check["duplicate"])                           # case-insensitive
        self.assertEqual(check["url"], self.url("detail", first.pk))
        self.create(**{self.duplicate_field: value})                  # "continue" still works
        self.assertEqual(self.model.objects.count(), 2)

    def test_duplicate_check_ignores_the_record_being_edited_and_deleted_ones(self):
        obj = self.make()
        value = getattr(obj, self.duplicate_field)
        self.assertFalse(self.client.get(
            self.url("check"), {"value": value, "exclude": obj.pk}).json()["duplicate"])
        self.assertFalse(self.client.get(self.url("check"), {"value": "Totally new"}).json()["duplicate"])
        self.assertFalse(self.client.get(self.url("check"), {"value": ""}).json()["duplicate"])
        obj.soft_delete()
        self.assertFalse(self.client.get(self.url("check"), {"value": value}).json()["duplicate"])

    # --- view -------------------------------------------------------------
    def test_detail_shows_the_record(self):
        obj = self.make()
        r = self.client.get(self.url("detail", obj.pk))
        self.assertContains(r, getattr(obj, self.name_field))
        self.assertContains(r, obj.image.url)

    def test_missing_record_redirects_to_list_with_a_message(self):
        for suffix in ("detail", "edit", "delete"):
            r = self.client.get(self.url(suffix, 99999), follow=True)
            self.assertRedirects(r, self.url("list"))
            self.assertContains(r, "no longer exists")

    # --- update -----------------------------------------------------------
    def test_update_changes_fields_and_audits_only_what_changed(self):
        obj = self.make()
        old_name = getattr(obj, self.name_field)
        data = self.current_values(obj)
        data[self.name_field] = "Renamed"                  # no image sent: keep the current one
        r = self.client.post(self.url("edit", obj.pk), data)
        self.assertRedirects(r, self.url("detail", obj.pk))
        obj.refresh_from_db()
        self.assertEqual(getattr(obj, self.name_field), "Renamed")
        self.assertTrue(obj.image)                         # image kept
        log = AuditLog.objects.get(action=AuditLog.UPDATE)
        self.assertEqual(log.record_type, self.record_type)
        self.assertTrue(log.details.startswith(f"{old_name}: "), log.details)
        self.assertNotIn("Reference image", log.details)   # image did not change

    def test_update_can_replace_the_image(self):
        obj = self.make()
        old = obj.image.name
        data = self.current_values(obj)
        data["image"] = png("new.png", colour=(200, 0, 0))
        self.client.post(self.url("edit", obj.pk), data)
        obj.refresh_from_db()
        self.assertNotEqual(obj.image.name, old)
        self.assertIn("Reference image", AuditLog.objects.get(action=AuditLog.UPDATE).details)

    def test_browser_style_line_breaks_do_not_count_as_a_change(self):
        """Regression: browsers send \\r\\n in text boxes. Editing one field must not
        report the multi-line fields as changed."""
        obj = self.make()
        multiline = [k for k in self.current_values(obj)
                     if k in ("identification_features", "description")][0]
        setattr(obj, multiline, "First line\nSecond line")
        obj.save()
        data = self.current_values(obj)
        data[multiline] = "First line\r\nSecond line"              # what a real browser sends
        data[self.name_field] = "Renamed"
        self.client.post(self.url("edit", obj.pk), data)
        details = AuditLog.objects.get(action=AuditLog.UPDATE).details
        self.assertNotIn(multiline.replace("_", " ").title(), details)
        self.assertEqual(details.count(","), 0, details)           # only one field changed

    def test_resubmitting_unchanged_browser_form_is_a_no_op(self):
        obj = self.make()
        data = self.current_values(obj)
        for k, v in data.items():
            if isinstance(v, str):
                data[k] = v.replace("\n", "\r\n")
        self.client.post(self.url("edit", obj.pk), data)
        self.assertFalse(AuditLog.objects.filter(action=AuditLog.UPDATE).exists())

    def test_update_with_no_change_writes_no_audit_entry(self):
        obj = self.make()
        r = self.client.post(self.url("edit", obj.pk), self.current_values(obj))
        self.assertRedirects(r, self.url("detail", obj.pk))
        self.assertFalse(AuditLog.objects.filter(action=AuditLog.UPDATE).exists())

    def test_update_validation_error_keeps_old_data(self):
        obj = self.make()
        data = self.current_values(obj)
        data[self.name_field] = ""
        r = self.client.post(self.url("edit", obj.pk), data)
        self.assertEqual(r.status_code, 200)
        obj.refresh_from_db()
        self.assertNotEqual(getattr(obj, self.name_field), "")
        self.assertFalse(AuditLog.objects.filter(action=AuditLog.UPDATE).exists())

    def test_update_of_a_record_deleted_meanwhile(self):
        obj = self.make()
        obj.soft_delete()
        r = self.client.post(self.url("edit", obj.pk), self.valid_data(), follow=True)
        self.assertRedirects(r, self.url("list"))
        self.assertContains(r, "no longer exists")

    # --- delete (soft) ----------------------------------------------------
    def test_delete_needs_confirmation_then_hides_the_record(self):
        obj = self.make()
        self.assertEqual(self.client.get(self.url("delete", obj.pk)).status_code, 200)
        self.assertFalse(self.model.all_objects.get(pk=obj.pk).is_deleted)   # GET deletes nothing

        r = self.client.post(self.url("delete", obj.pk))
        self.assertRedirects(r, self.url("list"))
        row = self.model.all_objects.get(pk=obj.pk)
        self.assertTrue(row.is_deleted)                    # kept in the database (risk R09)
        self.assertIsNotNone(row.deleted_at)
        self.assertEqual(self.model.objects.count(), 0)
        self.assertEqual(AuditLog.objects.get(action=AuditLog.DELETE).record_type, self.record_type)

    def test_deleted_record_disappears_from_list_search_and_detail(self):
        obj = self.make()
        needle = getattr(obj, self.search_field)
        self.client.post(self.url("delete", obj.pk))
        self.assertEqual(list(self.client.get(self.url("list")).context["rows"]), [])
        self.assertEqual(list(self.client.get(self.url("list"), {"q": needle}).context["rows"]), [])
        self.assertRedirects(self.client.get(self.url("detail", obj.pk)), self.url("list"))

    def test_deleting_twice_is_handled(self):
        obj = self.make()
        self.client.post(self.url("delete", obj.pk))
        r = self.client.post(self.url("delete", obj.pk), follow=True)
        self.assertContains(r, "no longer exists")
        self.assertEqual(AuditLog.objects.filter(action=AuditLog.DELETE).count(), 1)

    # --- list, search, paging --------------------------------------------
    def test_empty_list_message(self):
        self.assertContains(self.client.get(self.url("list")), "No records available yet.")

    def test_search_and_no_results(self):
        obj = self.make()
        needle = getattr(obj, self.search_field)[:4].lower()
        rows = self.client.get(self.url("list"), {"q": needle}).context["rows"]
        self.assertEqual([r["obj"].pk for r in rows], [obj.pk])
        self.assertContains(self.client.get(self.url("list"), {"q": "zzzzqqq"}), "No results found.")
        # empty search box shows everything
        self.assertEqual(len(self.client.get(self.url("list"), {"q": ""}).context["rows"]), 1)

    def test_pagination(self):
        for i in range(12):
            self.make(**{self.name_field: f"Record {i:02d}"})
        page1 = self.client.get(self.url("list")).context["page"]
        self.assertEqual(len(page1), 10)
        self.assertTrue(page1.has_next())
        page2 = self.client.get(self.url("list"), {"page": 2}).context["page"]
        self.assertEqual(len(page2), 2)

    def test_audit_log_page_filters_by_the_new_record_type(self):
        self.create()
        r = self.client.get(reverse("audit_logs"), {"record_type": self.record_type})
        self.assertEqual(len(r.context["page"]), 1)


@override_settings(MEDIA_ROOT=TEMP_MEDIA)
class BotanicalRecordTests(RecordCrudMixin, PortalTestCase):
    key = "botanical"
    model = BotanicalRecord
    record_type = "Botanical Record"
    name_field = "common_name"
    search_field = "family"
    duplicate_field = "scientific_name"

    def valid_data(self, **overrides):
        data = {
            "common_name": "Tembusu",
            "scientific_name": "Cyrtophyllum fragrans",
            "family": "Gentianaceae",
            "identification_features": "Dark fissured bark.\nSmall cream flowers.",
            "fun_fact": "Appears on the Singapore five-dollar note.",
            "status": "PUBLISHED",
            "image": png(),
        }
        data.update(overrides)
        return data

    def test_scientific_name_must_look_like_a_name(self):
        for bad in ("Ficus <script>", "Ficus 123", "!!!"):
            r = self.client.post(self.url("new"), self.valid_data(scientific_name=bad))
            self.assertIn("scientific_name", r.context["form"].errors, bad)
        self.assertEqual(BotanicalRecord.objects.count(), 0)
        for good in ("Vanda Miss Joaquim", "Ficus × benjamina", "Dendrobium 'Sonia'"):
            self.client.post(self.url("new"), self.valid_data(scientific_name=good, image=png()))
        self.assertEqual(BotanicalRecord.objects.count(), 3)

    def test_line_breaks_survive_but_extra_spaces_do_not(self):
        self.create(identification_features="  Dark   bark \n\n\n\n  Cream   flowers ")
        self.assertEqual(BotanicalRecord.objects.get().identification_features,
                         "Dark bark\n\nCream flowers")

    def test_search_matches_common_scientific_and_family(self):
        self.make()
        for needle in ("tembu", "fragrans", "gentian"):
            rows = self.client.get(self.url("list"), {"q": needle}).context["rows"]
            self.assertEqual(len(rows), 1, needle)


@override_settings(MEDIA_ROOT=TEMP_MEDIA)
class ArchitectureRecordTests(RecordCrudMixin, PortalTestCase):
    key = "architecture"
    model = ArchitectureRecord
    record_type = "Architecture Record"
    name_field = "name"
    search_field = "location"
    duplicate_field = "name"

    def valid_data(self, **overrides):
        data = {
            "name": "Burkill Hall",
            "location": "Near the Botanic Gardens Heritage Museum",
            "architectural_style": "Colonial bungalow",
            "construction_date": "1868",
            "description": "Two-storey house with a verandah and a pitched roof.",
            "fun_fact": "",
            "status": "PUBLISHED",
            "image": png(),
        }
        data.update(overrides)
        return data

    def test_fun_fact_is_optional(self):
        self.create(fun_fact="")
        self.assertEqual(ArchitectureRecord.objects.count(), 1)

    def test_search_matches_name_location_style_and_keyword(self):
        self.make()
        for needle in ("burkill", "heritage museum", "colonial", "verandah"):
            rows = self.client.get(self.url("list"), {"q": needle}).context["rows"]
            self.assertEqual(len(rows), 1, needle)


# --------------------------------------------------------------------------- sample data
@override_settings(MEDIA_ROOT=TEMP_MEDIA)
class SeedRecordsTests(PortalTestCase):
    def test_seed_creates_15_of_each_and_is_repeatable(self):
        call_command("seed_records", verbosity=0)
        call_command("seed_records", verbosity=0)
        self.assertEqual(BotanicalRecord.objects.count(), 15)
        self.assertEqual(ArchitectureRecord.objects.count(), 15)

    def test_seed_clear_removes_only_sample_records(self):
        call_command("seed_records", verbosity=0)
        real = BotanicalRecord(common_name="Real", scientific_name="Realus plantus",
                               family="Realaceae", identification_features="x", fun_fact="y")
        real.image.save("real-photo.png", png(), save=False)
        real.save()
        call_command("seed_records", "--clear", verbosity=0)
        self.assertEqual(list(BotanicalRecord.objects.values_list("common_name", flat=True)), ["Real"])
        self.assertEqual(ArchitectureRecord.objects.count(), 0)

    def test_sample_data_passes_the_apps_own_validation(self):
        from .forms import ArchitectureRecordForm, BotanicalRecordForm
        call_command("seed_records", verbosity=0)
        for r in BotanicalRecord.objects.all():
            f = BotanicalRecordForm({
                "common_name": r.common_name, "scientific_name": r.scientific_name,
                "family": r.family, "identification_features": r.identification_features,
                "fun_fact": r.fun_fact, "status": r.status}, instance=r)
            f.fields["image"].required = False
            self.assertTrue(f.is_valid(), (r, dict(f.errors)))
        for r in ArchitectureRecord.objects.all():
            f = ArchitectureRecordForm({
                "name": r.name, "location": r.location, "architectural_style": r.architectural_style,
                "construction_date": r.construction_date, "description": r.description,
                "fun_fact": r.fun_fact, "status": r.status}, instance=r)
            f.fields["image"].required = False
            self.assertTrue(f.is_valid(), (r, dict(f.errors)))
