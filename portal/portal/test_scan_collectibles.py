"""
Tests for the visitor's collectible inventory: scanning a plant with the PlantDex model
unlocks its collectible (CollectibleUnlock), and the app's catalog says which cards
that visitor has discovered.
"""
from unittest import mock

from django.contrib.auth.models import User
from django.core.files.uploadedfile import SimpleUploadedFile
from django.core.management import call_command

from .models import Collectible, CollectibleUnlock
from .test_collectibles import CollectibleTestCase


def photo():
    return SimpleUploadedFile("scan.jpg", b"jpegbytes", content_type="image/jpeg")


class ScanUnlockTests(CollectibleTestCase):
    def setUp(self):
        super().setUp()
        self.visitor = User.objects.get_or_create(username="visitor01")[0]
        self.card = self.collectible(name="Tanglin Gate", record=self.architecture("Tanglin Gate"),
                                     category=self.arch_cat, model_label="tanglin_gate")

    def scan(self, label="tanglin_gate", score=0.95, **data):
        with mock.patch("portal.api.classifier.available", return_value=True), \
             mock.patch("portal.api.classifier.classify", return_value=[(label, score)]):
            return self.client.post("/api/identify", {"image": photo(), **data}).json()

    # --- catalog ------------------------------------------------------------
    def test_catalog_lists_published_collectibles(self):
        self.collectible(name="Draft Card", status="DRAFT")
        items = self.client.get("/api/collectibles").json()["collectibles"]
        self.assertEqual([c["name"] for c in items], ["Tanglin Gate"])
        item = items[0]
        self.assertEqual((item["id"], item["category"], item["scientificName"]),
                         (str(self.card.pk), "Architecture", None))
        self.assertFalse(item["discovered"])
        self.assertTrue(item["imageUrl"].endswith(f"/api/collectibles/{self.card.pk}/image"))

    def test_catalog_hides_card_whose_record_is_no_longer_verified(self):
        record = self.botanical("Rain Tree", "Samanea saman")
        self.collectible(record=record, name="Rain Tree")
        record.status = "DRAFT"
        record.save()
        names = [c["name"] for c in self.client.get("/api/collectibles").json()["collectibles"]]
        self.assertNotIn("Rain Tree", names)

    def test_catalog_marks_cards_this_visitor_discovered(self):
        self.unlock(self.card, "visitor01")
        mine = self.client.get(f"/api/collectibles?userId={self.visitor.pk}").json()["collectibles"][0]
        self.assertTrue(mine["discovered"])
        self.assertIsNotNone(mine["unlockedAt"])
        someone_else = User.objects.get_or_create(username="visitor02")[0]
        theirs = self.client.get(f"/api/collectibles?userId={someone_else.pk}").json()["collectibles"][0]
        self.assertFalse(theirs["discovered"])

    def test_card_picture_is_served_without_login(self):
        self.client.logout()
        r = self.client.get(f"/api/collectibles/{self.card.pk}/image")
        self.assertEqual(r.status_code, 200)
        self.assertTrue(r["Content-Type"].startswith("image/"))

    def test_draft_card_picture_is_not_served(self):
        draft = self.collectible(name="Draft Card", status="DRAFT")
        self.assertEqual(self.client.get(f"/api/collectibles/{draft.pk}/image").status_code, 404)

    # --- scanning -----------------------------------------------------------
    def test_scan_unlocks_collectible_once(self):
        first = self.scan(userId=self.visitor.pk)
        self.assertTrue(first["newlyUnlocked"])
        self.assertEqual(first["collectible"]["id"], str(self.card.pk))
        self.assertTrue(first["collectible"]["discovered"])
        second = self.scan(userId=self.visitor.pk)
        self.assertFalse(second["newlyUnlocked"])
        self.assertEqual(CollectibleUnlock.objects.filter(visitor_username="visitor01").count(), 1)

    def test_scan_without_user_unlocks_nothing(self):
        r = self.scan()
        self.assertEqual(r["collectible"]["name"], "Tanglin Gate")
        self.assertFalse(r["newlyUnlocked"])
        self.assertFalse(CollectibleUnlock.objects.exists())

    def test_scan_of_class_without_collectible_still_identifies(self):
        r = self.scan(label="spathiphyllum", userId=self.visitor.pk)
        self.assertEqual(r["results"][0]["species"]["commonNames"], ["Peace Lily"])
        self.assertIsNone(r["collectible"])
        self.assertFalse(CollectibleUnlock.objects.exists())

    def test_unsure_scan_unlocks_nothing(self):
        with mock.patch.dict("os.environ", {}, clear=True):
            self.scan(score=0.3, userId=self.visitor.pk)
        self.assertFalse(CollectibleUnlock.objects.exists())

    # --- System Admin form --------------------------------------------------
    def test_scan_label_must_be_unique(self):
        record = self.botanical("Rain Tree", "Samanea saman")
        r = self.client.post("/section/collectibles/new/", self.data(record, model_label="Tanglin_Gate"))
        self.assertEqual(r.status_code, 200)
        self.assertContains(r, "already uses this scan label")


class SeedScanCollectiblesTests(CollectibleTestCase):
    def test_creates_one_card_per_model_class_and_is_repeatable(self):
        call_command("seed_scan_collectibles", stdout=mock.Mock())
        labels = set(Collectible.objects.exclude(model_label="").values_list("model_label", flat=True))
        self.assertEqual(len(labels), 13)
        self.assertIn("tanglin_gate", labels)
        call_command("seed_scan_collectibles", stdout=mock.Mock())
        self.assertEqual(Collectible.objects.count(), 13)

    def test_reuses_existing_collectible_for_same_record(self):
        existing = self.collectible(record=self.architecture("Tanglin Gate"), name="Main Gate",
                                    category=self.arch_cat)
        call_command("seed_scan_collectibles", stdout=mock.Mock())
        existing.refresh_from_db()
        self.assertEqual(existing.model_label, "tanglin_gate")
        self.assertEqual(Collectible.objects.filter(architecture_record=existing.architecture_record).count(), 1)
