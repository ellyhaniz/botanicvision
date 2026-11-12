"""
Tests for System Admin collectibles (SA-04 .. SA-08), the database picture store, and the
safeguard that stops a Researcher deleting a record a collectible depends on.
"""
from unittest import mock

from django.core.files.base import ContentFile
from django.core.files.uploadedfile import SimpleUploadedFile
from django.core.management import call_command
from django.core.management.base import CommandError
from django.db import IntegrityError, transaction
from django.db.models import ProtectedError
from django.test import override_settings
from django.urls import reverse
from django.utils.html import escape

from .models import (ArchitectureRecord, AuditLog, BotanicalRecord, Collectible, CollectibleCategory,
                     CollectibleUnlock, ProfileType, StoredFile)
from .storage import DatabaseStorage
from .test_records import TEMP_MEDIA, png
from .tests import PortalTestCase, client_for


@override_settings(MEDIA_ROOT=TEMP_MEDIA)          # the Researcher records still use a media folder
class CollectibleTestCase(PortalTestCase):
    def setUp(self):
        super().setUp()
        self.flora = CollectibleCategory.objects.get(name="Flora")
        self.arch_cat = CollectibleCategory.objects.get(name="Architecture")
        self.login("sysadmin")

    # --- fixtures ----------------------------------------------------------
    def botanical(self, name="Tembusu", sci="Cyrtophyllum fragrans", status="PUBLISHED", **kw):
        rec = BotanicalRecord(common_name=name, scientific_name=sci, family="Gentianaceae",
                              identification_features="Dark bark.\nCream flowers.", fun_fact="x",
                              status=status, **kw)
        rec.image.save("b.png", png(), save=False)
        rec.save()
        return rec

    def architecture(self, name="Burkill Hall", status="PUBLISHED", **kw):
        rec = ArchitectureRecord(name=name, location="Lawn A", architectural_style="Colonial",
                                 construction_date="1868", description="Verandah.", status=status, **kw)
        rec.image.save("a.png", png(), save=False)
        rec.save()
        return rec

    def collectible(self, record=None, name=None, **kw):
        record = record or self.botanical()
        link = ({"botanical_record": record} if isinstance(record, BotanicalRecord)
                else {"architecture_record": record})
        c = Collectible(name=name or f"Card {record}", description="A card.", category=kw.pop("category", self.flora),
                        rarity=kw.pop("rarity", "COMMON"), status=kw.pop("status", "PUBLISHED"), **link, **kw)
        c.image.save("c.png", png(), save=False)
        c.save()
        return c

    def unlock(self, collectible, username="visitor01"):
        return CollectibleUnlock.objects.create(collectible=collectible, visitor_username=username)

    def link_value(self, record):
        return f"{'b' if isinstance(record, BotanicalRecord) else 'a'}:{record.pk}"

    def data(self, record, **overrides):
        data = {"name": "Tembusu card", "linked_record": self.link_value(record),
                "category": self.flora.pk, "rarity": "RARE", "description": "Spot the Tembusu.",
                "status": "PUBLISHED", "image": png("card.png")}
        data.update(overrides)
        return data

    def unchanged(self, c):
        """Form data that re-submits a collectible unchanged (no new image)."""
        return {"name": c.name, "linked_record": self.link_value(c.linked_record),
                "category": c.category.pk, "rarity": c.rarity, "description": c.description,
                "status": c.status}


# =========================================================================== access
class CollectibleAccessTests(CollectibleTestCase):
    def test_default_categories_exist_after_migrating(self):
        self.assertEqual(set(CollectibleCategory.objects.values_list("name", flat=True)),
                         {"Flora", "Architecture"})

    def test_only_system_admin_can_open_the_pages(self):
        c = self.collectible()
        urls = [reverse("collectible_list"), reverse("collectible_new"),
                reverse("collectible_detail", args=[c.pk]), reverse("collectible_edit", args=[c.pk]),
                reverse("collectible_delete", args=[c.pk])]
        for url in urls:
            self.assertEqual(self.client.get(url).status_code, 200, url)
            for other in ("researcher", "useradmin", "visitor01"):
                self.assertEqual(client_for(other).get(url).status_code, 403, f"{other} {url}")
            anonymous = self.client_class().get(url)
            self.assertEqual(anonymous.status_code, 302, url)
            self.assertIn("/login/", anonymous["Location"])

    def test_other_roles_cannot_post_either(self):
        c = self.collectible()
        for other in ("researcher", "useradmin"):
            other_client = client_for(other)
            self.assertEqual(other_client.post(reverse("collectible_new"),
                                               self.data(self.botanical("Rain Tree", "Samanea saman"))).status_code, 403)
            self.assertEqual(other_client.post(reverse("collectible_delete", args=[c.pk])).status_code, 403)
        self.assertEqual(Collectible.objects.count(), 1)

    def test_permission_comes_from_the_profile_type(self):
        pt = ProfileType.objects.get(group__name="System Admin")
        pt.permissions.remove(*pt.permissions.filter(code="collectibles"))
        self.assertEqual(self.client.get(reverse("collectible_list")).status_code, 403)

    def test_dashboard_card_opens_the_real_page(self):
        r = self.client.get("/section/collectibles/")
        self.assertNotContains(r, "under construction")
        self.assertEqual(r.resolver_match.url_name, "collectible_list")


# =========================================================================== create (SA-04)
class CollectibleCreateTests(CollectibleTestCase):
    def post(self, record, **overrides):
        return self.client.post(reverse("collectible_new"), self.data(record, **overrides))

    def test_create_linked_to_a_botanical_record(self):
        rec = self.botanical()
        r = self.post(rec)
        c = Collectible.objects.get()
        self.assertRedirects(r, reverse("collectible_detail", args=[c.pk]))
        self.assertEqual((c.botanical_record, c.architecture_record), (rec, None))
        self.assertEqual((c.created_by, c.rarity, c.category), (self.sysadmin, "RARE", self.flora))
        log = AuditLog.objects.get(action=AuditLog.CREATE)
        self.assertEqual(log.record_type, "Collectible")
        self.assertIn("Tembusu card", log.details)
        self.assertIn("Tembusu", log.details)               # names the linked record

    def test_create_linked_to_an_architecture_record(self):
        rec = self.architecture()
        self.post(rec, category=self.arch_cat.pk, name="Burkill card")
        c = Collectible.objects.get()
        self.assertEqual((c.architecture_record, c.botanical_record), (rec, None))
        self.assertEqual(c.linked_kind, "Architecture")

    def test_picture_is_stored_in_the_database_and_served(self):
        self.post(self.botanical())
        c = Collectible.objects.get()
        stored = StoredFile.objects.get(name=c.image.name)
        self.assertTrue(c.image.name.startswith("collectibles/"))
        self.assertGreater(stored.size, 0)
        resp = self.client.get(c.image.url)
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(resp["Content-Type"], "image/png")
        self.assertEqual(resp.content, bytes(stored.content))

    def test_missing_fields_write_nothing(self):
        r = self.client.post(reverse("collectible_new"), {})
        self.assertEqual(r.status_code, 200)
        errors = r.context["form"].errors
        for field in ("name", "linked_record", "category", "description", "image", "status"):
            self.assertIn(field, errors, field)
        self.assertEqual(Collectible.objects.count(), 0)
        self.assertFalse(AuditLog.objects.filter(action=AuditLog.CREATE).exists())

    def test_image_is_required(self):
        data = self.data(self.botanical())
        del data["image"]
        r = self.client.post(reverse("collectible_new"), data)
        self.assertIn("image", r.context["form"].errors)

    def test_rejects_wrong_file_type_and_extension_and_size(self):
        rec = self.botanical()
        bad = SimpleUploadedFile("notes.txt", b"not an image", content_type="text/plain")
        self.assertIn("image", self.post(rec, image=bad).context["form"].errors)
        self.assertIn("JPG, PNG or WebP",
                      str(self.post(rec, image=png("pic.gif")).context["form"].errors["image"]))
        with mock.patch("portal.forms.MAX_IMAGE_MB", 0.00001):
            self.assertIn("smaller", str(self.post(rec).context["form"].errors["image"]))
        self.assertEqual(Collectible.objects.count(), 0)
        self.assertFalse(StoredFile.objects.filter(name__startswith="collectibles/").exists())   # nothing half-saved

    def test_unverified_record_is_rejected(self):
        draft = self.botanical(status="DRAFT")
        r = self.post(draft)
        self.assertContains(r, "not verified yet")
        self.assertEqual(Collectible.objects.count(), 0)

    def test_duplicate_collectible_for_a_record_is_rejected(self):
        rec = self.botanical()
        first = self.collectible(rec, name="First")
        r = self.post(rec)
        self.assertContains(r, "A collectible already exists for this record")
        self.assertContains(r, "First")
        self.assertEqual(Collectible.objects.count(), 1)

    def test_record_removed_meanwhile_is_rejected(self):
        rec = self.botanical()
        value = self.link_value(rec)
        rec.soft_delete()
        r = self.post(rec, linked_record=value)
        self.assertContains(r, "no longer exists")
        self.assertEqual(Collectible.objects.count(), 0)

    def test_garbage_link_value_is_rejected(self):
        for bad in ("x:1", "b:abc", "b:99999", "nonsense"):
            r = self.post(self.botanical(), linked_record=bad)
            self.assertIn("linked_record", r.context["form"].errors, bad)

    def test_two_collectibles_can_link_two_different_records(self):
        self.post(self.botanical("Tembusu", "Cyrtophyllum fragrans"), name="A")
        self.post(self.botanical("Rain Tree", "Samanea saman"), name="B")
        self.post(self.architecture(), name="C", category=self.arch_cat.pk)
        self.post(self.architecture("Bandstand"), name="D", category=self.arch_cat.pk)
        self.assertEqual(Collectible.objects.count(), 4)

    def test_dropdown_marks_records_that_cannot_be_chosen(self):
        taken = self.botanical("Taken", "Aaa aaa")
        self.collectible(taken)
        self.botanical("Draftly", "Bbb bbb", status="DRAFT")
        self.botanical("Free", "Ccc ccc")
        labels = [label for _, group in self.client.get(reverse("collectible_new")).context["form"]
                  .fields["linked_record"].choices if isinstance(group, list) for _, label in group]
        text = " | ".join(labels)
        self.assertIn("Taken (Aaa aaa)  (already has a collectible)", text)
        self.assertIn("Draftly (Bbb bbb)  (Draft, not verified)", text)
        self.assertIn("Free (Ccc ccc)", text)

    def test_deleted_records_are_not_in_the_dropdown(self):
        rec = self.botanical("Gone", "Ddd ddd")
        rec.soft_delete()
        choices = self.client.get(reverse("collectible_new")).context["form"].fields["linked_record"].choices
        self.assertNotIn("Gone", str(choices))

    def test_text_is_tidied_and_line_breaks_kept(self):
        self.post(self.botanical(), name="  Spaced    out  ", description="  one   line \n\n\n\n second ")
        c = Collectible.objects.get()
        self.assertEqual((c.name, c.description), ("Spaced out", "one line\n\nsecond"))

    def test_database_refuses_a_collectible_with_both_or_no_link(self):
        b, a = self.botanical(), self.architecture()
        with self.assertRaises(IntegrityError), transaction.atomic():
            Collectible.objects.create(name="both", description="x", category=self.flora,
                                       image="collectibles/x.png", botanical_record=b, architecture_record=a)
        with self.assertRaises(IntegrityError), transaction.atomic():
            Collectible.objects.create(name="none", description="x", category=self.flora,
                                       image="collectibles/x.png")

    def test_database_refuses_two_collectibles_for_one_record(self):
        rec = self.botanical()
        self.collectible(rec)
        with self.assertRaises(IntegrityError), transaction.atomic():
            Collectible.objects.create(name="dup", description="x", category=self.flora,
                                       image="collectibles/x.png", botanical_record=rec)


# =========================================================================== view (SA-05)
class CollectibleViewTests(CollectibleTestCase):
    def test_detail_shows_the_collectible(self):
        c = self.collectible(name="Tembusu card", rarity="HIDDEN")
        r = self.client.get(reverse("collectible_detail", args=[c.pk]))
        for text in ("Tembusu card", "Hidden", "Flora", "Botanical record", c.image.url):
            self.assertContains(r, text)
        self.assertNotContains(r, "linked record is unavailable")

    def test_unlock_count_is_shown(self):
        c = self.collectible()
        self.unlock(c, "visitor01"); self.unlock(c, "visitor02")
        r = self.client.get(reverse("collectible_detail", args=[c.pk]))
        self.assertEqual(r.context["unlock_count"], 2)
        self.assertContains(r, "2 visitors")

    def test_linked_record_unavailable_is_flagged_but_details_still_show(self):
        deleted = self.collectible(self.botanical("Gone", "Eee eee"), name="Orphan A")
        deleted.botanical_record.soft_delete()
        draft = self.collectible(self.botanical("Slipped", "Fff fff"), name="Orphan B")
        BotanicalRecord.objects.filter(pk=draft.botanical_record_id).update(status="DRAFT")
        for c in (deleted, draft):
            r = self.client.get(reverse("collectible_detail", args=[c.pk]))
            self.assertContains(r, c.name)
            self.assertContains(r, "linked record is unavailable")
        self.assertEqual(self.client.get(reverse("collectible_list")).content.decode().count(">unavailable<"), 2)

    def test_missing_collectible_redirects_with_a_message(self):
        for suffix in ("detail", "edit", "delete"):
            r = self.client.get(reverse(f"collectible_{suffix}", args=[99999]), follow=True)
            self.assertRedirects(r, reverse("collectible_list"))
            self.assertContains(r, "no longer exists")


# =========================================================================== update (SA-06)
class CollectibleUpdateTests(CollectibleTestCase):
    def edit(self, c, **overrides):
        data = self.unchanged(c)
        data.update(overrides)
        return self.client.post(reverse("collectible_edit", args=[c.pk]), data)

    def test_update_fields_and_audit_lists_only_what_changed(self):
        c = self.collectible(name="Old name", rarity="COMMON")
        r = self.edit(c, rarity="RARE", category=self.arch_cat.pk)
        self.assertRedirects(r, reverse("collectible_detail", args=[c.pk]))
        c.refresh_from_db()
        self.assertEqual((c.rarity, c.category), ("RARE", self.arch_cat))
        log = AuditLog.objects.get(action=AuditLog.UPDATE)
        self.assertEqual(log.record_type, "Collectible")
        self.assertTrue(log.details.startswith("Old name: "), log.details)
        self.assertIn("Rarity level", log.details)
        self.assertIn("Category", log.details)
        self.assertNotIn("Card image", log.details)
        self.assertNotIn("Linked record", log.details)

    def test_replacing_the_image_removes_the_old_picture(self):
        c = self.collectible()
        old = c.image.name
        data = self.unchanged(c)
        data["image"] = png("new.png", colour=(200, 0, 0))
        self.client.post(reverse("collectible_edit", args=[c.pk]), data)
        c.refresh_from_db()
        self.assertNotEqual(c.image.name, old)
        self.assertTrue(StoredFile.objects.filter(name=c.image.name).exists())
        self.assertFalse(StoredFile.objects.filter(name=old).exists())        # no clutter left in the database
        self.assertIn("Card image", AuditLog.objects.get(action=AuditLog.UPDATE).details)

    def test_relinking_frees_the_old_record(self):
        old_rec, new_rec = self.botanical("Old", "Ggg ggg"), self.botanical("New", "Hhh hhh")
        c = self.collectible(old_rec)
        self.edit(c, linked_record=self.link_value(new_rec))
        c.refresh_from_db()
        self.assertEqual(c.botanical_record, new_rec)
        self.assertIn("Linked record", AuditLog.objects.get(action=AuditLog.UPDATE).details)
        self.collectible(old_rec, name="Another for the old record")           # now allowed
        self.assertEqual(Collectible.objects.count(), 2)

    def test_relinking_to_another_kind_of_record(self):
        c = self.collectible(self.botanical())
        arch = self.architecture()
        self.edit(c, linked_record=self.link_value(arch), category=self.arch_cat.pk)
        c.refresh_from_db()
        self.assertEqual((c.botanical_record, c.architecture_record), (None, arch))

    def test_relinking_to_a_record_that_already_has_a_collectible_is_rejected(self):
        other = self.collectible(self.botanical("Taken", "Iii iii"), name="Holder")
        c = self.collectible(self.botanical("Mine", "Jjj jjj"), name="Mine")
        r = self.edit(c, linked_record=self.link_value(other.botanical_record))
        self.assertContains(r, "A collectible already exists for this record")
        self.assertContains(r, "Holder")
        c.refresh_from_db()
        self.assertEqual(c.botanical_record.common_name, "Mine")

    def test_relinking_to_an_unverified_record_is_rejected(self):
        c = self.collectible()
        draft = self.botanical("Draftly", "Kkk kkk", status="DRAFT")
        r = self.edit(c, linked_record=self.link_value(draft))
        self.assertContains(r, "not verified yet")

    def test_keeping_the_same_link_is_not_flagged_as_a_duplicate(self):
        c = self.collectible()
        r = self.edit(c, rarity="UNCOMMON")
        self.assertEqual(r.status_code, 302)

    def test_no_change_writes_no_audit_entry(self):
        c = self.collectible()
        r = self.client.post(reverse("collectible_edit", args=[c.pk]), self.unchanged(c), follow=True)
        self.assertContains(r, "No changes to save.")
        self.assertFalse(AuditLog.objects.filter(action=AuditLog.UPDATE).exists())

    def test_browser_style_line_breaks_are_not_a_change(self):
        c = self.collectible()
        c.description = "First line\nSecond line"
        c.save()
        data = self.unchanged(c)
        data["description"] = "First line\r\nSecond line"
        self.client.post(reverse("collectible_edit", args=[c.pk]), data)
        self.assertFalse(AuditLog.objects.filter(action=AuditLog.UPDATE).exists())

    def test_invalid_input_keeps_old_data(self):
        c = self.collectible(name="Keep me")
        r = self.edit(c, name="")
        self.assertEqual(r.status_code, 200)
        c.refresh_from_db()
        self.assertEqual(c.name, "Keep me")
        self.assertFalse(AuditLog.objects.filter(action=AuditLog.UPDATE).exists())

    def test_warning_when_visitors_already_unlocked_it(self):
        c = self.collectible()
        self.unlock(c)
        self.assertContains(self.client.get(reverse("collectible_edit", args=[c.pk])), "already unlocked")
        self.assertNotContains(self.client.get(reverse("collectible_edit", args=[self.collectible(
            self.botanical("Free", "Lll lll")).pk])), "already unlocked")

    def test_update_of_a_collectible_deleted_meanwhile(self):
        c = self.collectible()
        data, pk = self.unchanged(c), c.pk
        c.delete()
        r = self.client.post(reverse("collectible_edit", args=[pk]), data, follow=True)
        self.assertRedirects(r, reverse("collectible_list"))
        self.assertContains(r, "no longer exists")


# =========================================================================== delete (SA-07)
class CollectibleDeleteTests(CollectibleTestCase):
    def test_delete_needs_confirmation_then_removes_it_and_its_picture(self):
        c = self.collectible(name="Doomed")
        picture = c.image.name
        url = reverse("collectible_delete", args=[c.pk])
        self.assertEqual(self.client.get(url).status_code, 200)
        self.assertTrue(Collectible.objects.filter(pk=c.pk).exists())       # GET deletes nothing

        r = self.client.post(url)
        self.assertRedirects(r, reverse("collectible_list"))
        self.assertFalse(Collectible.objects.filter(pk=c.pk).exists())
        self.assertFalse(StoredFile.objects.filter(name=picture).exists())
        log = AuditLog.objects.get(action=AuditLog.DELETE)
        self.assertEqual((log.record_type, log.details), ("Collectible", "Doomed"))

    def test_linked_record_survives_and_can_get_a_new_collectible(self):
        rec = self.botanical()
        c = self.collectible(rec)
        self.client.post(reverse("collectible_delete", args=[c.pk]))
        self.assertTrue(BotanicalRecord.objects.filter(pk=rec.pk).exists())
        self.collectible(rec, name="Replacement")
        self.assertEqual(Collectible.objects.get().name, "Replacement")

    def test_collectible_in_use_cannot_be_deleted(self):
        c = self.collectible(name="Popular")
        self.unlock(c, "visitor01"); self.unlock(c, "visitor02")
        url = reverse("collectible_delete", args=[c.pk])

        r = self.client.get(url, follow=True)                    # the confirmation page is never offered
        self.assertRedirects(r, reverse("collectible_detail", args=[c.pk]))
        self.assertContains(r, "unlocked by 2 visitors")
        self.assertContains(r, "Set its status to Draft")

        r = self.client.post(url, follow=True)
        self.assertContains(r, "cannot be deleted")
        self.assertTrue(Collectible.objects.filter(pk=c.pk).exists())
        self.assertTrue(StoredFile.objects.filter(name=c.image.name).exists())
        self.assertEqual(CollectibleUnlock.objects.count(), 2)   # visitor progress untouched
        self.assertFalse(AuditLog.objects.filter(action=AuditLog.DELETE).exists())

    def test_unlock_arriving_during_the_delete_is_still_protected(self):
        c = self.collectible()
        with mock.patch.object(Collectible, "delete", side_effect=ProtectedError("in use", set())):
            r = self.client.post(reverse("collectible_delete", args=[c.pk]), follow=True)
        self.assertContains(r, "cannot be deleted")
        self.assertTrue(Collectible.objects.filter(pk=c.pk).exists())
        self.assertFalse(AuditLog.objects.filter(action=AuditLog.DELETE).exists())

    def test_database_itself_protects_unlocked_collectibles(self):
        c = self.collectible()
        self.unlock(c)
        with self.assertRaises(ProtectedError):
            c.delete()

    def test_hiding_is_the_alternative_to_deleting(self):
        c = self.collectible()
        self.unlock(c)
        data = {"name": c.name, "linked_record": self.link_value(c.linked_record), "category": c.category.pk,
                "rarity": c.rarity, "description": c.description, "status": "DRAFT"}
        self.client.post(reverse("collectible_edit", args=[c.pk]), data)
        c.refresh_from_db()
        self.assertEqual(c.status, "DRAFT")

    def test_deleting_twice_is_handled(self):
        c = self.collectible()
        self.client.post(reverse("collectible_delete", args=[c.pk]))
        r = self.client.post(reverse("collectible_delete", args=[c.pk]), follow=True)
        self.assertContains(r, "no longer exists")
        self.assertEqual(AuditLog.objects.filter(action=AuditLog.DELETE).count(), 1)


# =========================================================================== search + filter (SA-08)
class CollectibleSearchTests(CollectibleTestCase):
    def setUp(self):
        super().setUp()
        self.tembusu = self.collectible(self.botanical("Tembusu", "Cyrtophyllum fragrans"), name="Tembusu card",
                                        rarity="RARE", status="PUBLISHED")
        self.burkill = self.collectible(self.architecture("Burkill Hall"), name="Colonial house card",
                                        rarity="COMMON", status="DRAFT", category=self.arch_cat)
        self.url = reverse("collectible_list")

    def names(self, **params):
        return {c.name for c in self.client.get(self.url, params).context["page"]}

    def test_keyword_matches_name_description_and_linked_record(self):
        self.assertEqual(self.names(q="tembusu card"), {"Tembusu card"})
        self.assertEqual(self.names(q="FRAGRANS"), {"Tembusu card"})           # linked scientific name
        self.assertEqual(self.names(q="burkill"), {"Colonial house card"})      # linked structure name
        self.assertEqual(self.names(q="a card."), {"Tembusu card", "Colonial house card"})   # description

    def test_filters(self):
        self.assertEqual(self.names(category=self.arch_cat.pk), {"Colonial house card"})
        self.assertEqual(self.names(rarity="RARE"), {"Tembusu card"})
        self.assertEqual(self.names(status="DRAFT"), {"Colonial house card"})
        self.assertEqual(self.names(kind="botanical"), {"Tembusu card"})
        self.assertEqual(self.names(kind="architecture"), {"Colonial house card"})
        self.assertEqual(self.names(kind="botanical", status="DRAFT"), set())    # combined

    def test_ignores_junk_filter_values(self):
        self.assertEqual(len(self.names(category="abc", rarity="zzz", status="nope", kind="what")), 2)

    def test_no_match_and_empty_search_messages(self):
        self.assertContains(self.client.get(self.url, {"q": "zzzzqqq"}), "No matching collectibles were found.")
        self.assertEqual(len(self.names(q="")), 2)                              # empty search shows everything
        Collectible.objects.all().delete()
        self.assertContains(self.client.get(self.url), "No collectibles available yet.")

    def test_unlock_count_is_not_inflated_by_the_joins(self):
        for visitor in ("visitor01", "visitor02", "visitor03"):
            self.unlock(self.tembusu, visitor)
        rows = {c.name: c.unlock_count for c in self.client.get(self.url).context["page"]}
        self.assertEqual(rows, {"Tembusu card": 3, "Colonial house card": 0})

    def test_pagination_keeps_the_filter(self):
        for i in range(11):
            self.collectible(self.botanical(f"Plant {i}", f"Plantus numerus{chr(97 + i)}"), name=f"Extra {i:02d}")
        page1 = self.client.get(self.url, {"q": "extra"}).context
        self.assertEqual((len(page1["page"]), page1["page"].paginator.count), (10, 11))
        self.assertContains(self.client.get(self.url, {"q": "extra"}), "?q=extra&page=2")
        self.assertEqual(len(self.client.get(self.url, {"q": "extra", "page": 2}).context["page"]), 1)


# =========================================================================== Researcher safeguard
class ResearcherSafeguardTests(CollectibleTestCase):
    def test_researcher_cannot_delete_a_record_a_collectible_depends_on(self):
        researcher = client_for("researcher")
        cases = [(self.botanical(), "botanical"), (self.architecture(), "architecture")]
        for rec, key in cases:
            self.collectible(rec, name=f"{key} card")
            for method in (researcher.get, researcher.post):
                r = method(reverse(f"{key}_delete", args=[rec.pk]), follow=True)
                self.assertRedirects(r, reverse(f"{key}_detail", args=[rec.pk]))
                self.assertContains(r, escape(f"is used by the collectible '{key} card'"))
            self.assertFalse(type(rec).all_objects.get(pk=rec.pk).is_deleted)
        self.assertFalse(AuditLog.objects.filter(action=AuditLog.DELETE).exists())

    def test_researcher_can_delete_once_the_collectible_is_gone(self):
        researcher = client_for("researcher")
        rec = self.botanical()
        c = self.collectible(rec)
        c.delete()
        r = researcher.post(reverse("botanical_delete", args=[rec.pk]))
        self.assertRedirects(r, reverse("botanical_list"))
        self.assertTrue(BotanicalRecord.all_objects.get(pk=rec.pk).is_deleted)

    def test_records_without_a_collectible_are_unaffected(self):
        researcher = client_for("researcher")
        rec = self.botanical()
        self.assertEqual(researcher.get(reverse("botanical_delete", args=[rec.pk])).status_code, 200)


# =========================================================================== database picture store
class DatabaseStorageTests(CollectibleTestCase):
    def test_save_open_exists_size_delete(self):
        storage = DatabaseStorage()
        name = storage.save("collectibles/2026/10/x.png", ContentFile(b"abc123"))
        self.assertTrue(storage.exists(name))
        self.assertEqual(storage.size(name), 6)
        self.assertEqual(storage.open(name).read(), b"abc123")
        self.assertEqual(storage.url(name), "/files/collectibles/2026/10/x.png")
        storage.delete(name)
        self.assertFalse(storage.exists(name))
        with self.assertRaises(FileNotFoundError):
            storage.open(name)
        storage.delete("never/existed.png")                    # deleting a missing file is harmless

    def test_same_file_name_does_not_overwrite(self):
        storage = DatabaseStorage()
        first = storage.save("collectibles/x.png", ContentFile(b"one"))
        second = storage.save("collectibles/x.png", ContentFile(b"two"))
        self.assertNotEqual(first, second)
        self.assertEqual(storage.open(first).read(), b"one")
        self.assertEqual(storage.open(second).read(), b"two")

    def test_file_view_requires_login(self):
        StoredFile.objects.create(name="collectibles/p.png", content=b"x", content_type="image/png", size=1)
        anonymous = self.client_class().get("/files/collectibles/p.png")
        self.assertEqual(anonymous.status_code, 302)
        self.assertIn("/login/", anonymous["Location"])

    def test_file_view_headers_and_missing_file(self):
        c = self.collectible()
        r = self.client.get(c.image.url)
        self.assertEqual(r.status_code, 200)
        self.assertEqual(r["X-Content-Type-Options"], "nosniff")
        self.assertIn("max-age", r["Cache-Control"])
        self.assertEqual(self.client.get("/files/collectibles/nothing.png").status_code, 404)

    def test_file_view_supports_conditional_requests(self):
        c = self.collectible()
        etag = self.client.get(c.image.url)["ETag"]
        again = self.client.get(c.image.url, headers={"If-None-Match": etag})
        self.assertEqual(again.status_code, 304)
        self.assertEqual(again.content, b"")

    def test_non_images_are_only_ever_a_download(self):
        StoredFile.objects.create(name="collectibles/evil.html", content=b"<script>alert(1)</script>",
                                  content_type="text/html", size=25)
        r = self.client.get("/files/collectibles/evil.html")
        self.assertEqual(r["Content-Type"], "application/octet-stream")
        self.assertEqual(r["Content-Disposition"], "attachment")

    def test_picture_survives_without_any_media_folder(self):
        """The whole point: the picture is in the database, not on this computer's disk."""
        with override_settings(MEDIA_ROOT="/nonexistent/folder"):
            c = self.collectible()
            self.assertEqual(self.client.get(c.image.url).status_code, 200)


# =========================================================================== sample data
class SeedCollectiblesTests(CollectibleTestCase):
    def seed(self):
        call_command("seed_records", verbosity=0)
        call_command("seed_collectibles", verbosity=0)

    def test_creates_one_collectible_per_published_record_and_is_repeatable(self):
        self.seed()
        published = (BotanicalRecord.objects.filter(status="PUBLISHED").count()
                     + ArchitectureRecord.objects.filter(status="PUBLISHED").count())
        self.assertEqual(Collectible.objects.count(), published)
        self.assertEqual(Collectible.objects.filter(status="DRAFT").count(), 0)
        call_command("seed_collectibles", verbosity=0)
        self.assertEqual(Collectible.objects.count(), published)
        self.assertTrue(CollectibleUnlock.objects.exists())                  # some are "in use"
        for c in Collectible.objects.all():
            self.assertTrue(StoredFile.objects.filter(name=c.image.name).exists())

    def test_sample_collectibles_pass_the_apps_own_validation(self):
        from .collectible_forms import CollectibleForm
        self.seed()
        for c in Collectible.objects.all():
            form = CollectibleForm({"name": c.name, "linked_record": self.link_value(c.linked_record),
                                    "category": c.category.pk, "rarity": c.rarity,
                                    "description": c.description, "status": c.status}, instance=c)
            form.fields["image"].required = False
            self.assertTrue(form.is_valid(), (c, dict(form.errors)))

    def test_clear_removes_only_sample_collectibles_unlocks_and_pictures(self):
        self.seed()
        mine = self.collectible(self.botanical("Real", "Realus plantus"), name="Real card")
        call_command("seed_collectibles", "--clear", verbosity=0)
        self.assertEqual(list(Collectible.objects.values_list("name", flat=True)), ["Real card"])
        self.assertEqual(CollectibleUnlock.objects.count(), 0)
        self.assertEqual(StoredFile.objects.filter(name__startswith="collectibles/").count(), 1)
        self.assertTrue(StoredFile.objects.filter(name=mine.image.name).exists())

    def test_clearing_records_first_is_refused_with_a_helpful_message(self):
        self.seed()
        with self.assertRaisesMessage(CommandError, "seed_collectibles --clear"):
            call_command("seed_records", "--clear", verbosity=0)
        call_command("seed_collectibles", "--clear", verbosity=0)
        call_command("seed_records", "--clear", verbosity=0)
        self.assertEqual(BotanicalRecord.objects.count(), 0)
