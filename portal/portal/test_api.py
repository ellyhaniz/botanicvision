"""
Tests for the Android app's JSON API (api.py).

Run with:   python manage.py test
"""
import json
from unittest import mock

from django.contrib.auth.models import User
from django.core.files.uploadedfile import SimpleUploadedFile
from django.core.management import call_command
from django.test import TestCase

from .models import AuditLog, BotanicalRecord

PW = "ChangeMe123!"


class ApiTestCase(TestCase):
    def setUp(self):
        call_command("seed_roles", verbosity=0)

    def post(self, url, data):
        return self.client.post(url, json.dumps(data), content_type="application/json")


class RegisterLoginTests(ApiTestCase):
    def test_register_creates_visitor_who_can_log_in(self):
        r = self.post("/api/auth/register", {"email": "v@sbg.test", "fullName": "Vi Sitor",
                                             "username": "visitor01", "password": "secret1"})
        self.assertEqual(r.status_code, 201)

        r = self.post("/api/auth/login", {"username": "visitor01", "password": "secret1"})
        self.assertEqual(r.status_code, 200)
        body = r.json()
        self.assertEqual(body["role"], "VISITOR")
        self.assertEqual(body["fullName"], "Vi Sitor")
        self.assertEqual(body["accountStatus"], "ACTIVE")
        for key in ("userId", "username", "profileTypeId", "profileTypeName", "email", "memberSince"):
            self.assertIn(key, body)
        self.assertTrue(AuditLog.objects.filter(username="visitor01", action=AuditLog.LOGIN).exists())

    def test_register_rejects_taken_username_and_missing_fields(self):
        self.assertEqual(self.post("/api/auth/register", {"email": "x@sbg.test", "fullName": "X",
                                                          "username": "researcher", "password": "p"}).status_code, 409)
        self.assertEqual(self.post("/api/auth/register", {"username": "new"}).status_code, 400)

    def test_researcher_gets_researcher_role(self):
        r = self.post("/api/auth/login", {"username": "researcher", "password": PW})
        self.assertEqual(r.json()["role"], "RESEARCHER")

    def test_wrong_password_and_inactive_account(self):
        r = self.post("/api/auth/login", {"username": "researcher", "password": "nope"})
        self.assertEqual(r.status_code, 401)
        self.assertIn("message", r.json())

        User.objects.filter(username="researcher").update(is_active=False)
        r = self.post("/api/auth/login", {"username": "researcher", "password": PW})
        self.assertEqual(r.status_code, 403)

    def test_logout(self):
        uid = User.objects.get(username="researcher").pk
        self.assertEqual(self.post("/api/auth/logout", {"userId": uid}).status_code, 200)
        self.assertEqual(self.post("/api/auth/logout", {"userId": 99999}).status_code, 404)


class AccountSelfServiceTests(ApiTestCase):
    def setUp(self):
        super().setUp()
        self.user = User.objects.create_user("visitor01", "v@sbg.test", "secret1")

    def test_reset_password(self):
        r = self.post("/api/auth/reset-password", {"email": "V@sbg.test", "username": "visitor01",
                                                   "newPassword": "newpass"})
        self.assertEqual(r.status_code, 200)
        self.user.refresh_from_db()
        self.assertTrue(self.user.check_password("newpass"))

    def test_reset_password_rejects_short_or_unknown(self):
        self.assertEqual(self.post("/api/auth/reset-password", {"email": "v@sbg.test", "username": "visitor01",
                                                                "newPassword": "abc"}).status_code, 400)
        self.assertEqual(self.post("/api/auth/reset-password", {"email": "other@sbg.test", "username": "visitor01",
                                                                "newPassword": "newpass"}).status_code, 404)

    def test_update_my_profile(self):
        r = self.client.put(f"/api/users/{self.user.pk}", json.dumps({"fullName": "New Name", "email": "n@sbg.test"}),
                            content_type="application/json")
        self.assertEqual(r.status_code, 200)
        self.user.refresh_from_db()
        self.assertEqual((self.user.get_full_name(), self.user.email), ("New Name", "n@sbg.test"))


class CollectibleTests(ApiTestCase):
    def test_starter_catalog_when_no_records(self):
        items = self.client.get("/api/collectibles").json()["collectibles"]
        self.assertEqual(items[0]["id"], "0042")

    def test_published_records_become_collectibles(self):
        image = SimpleUploadedFile("t.gif", b"GIF89a\x01\x00\x01\x00\x00\x00\x00;", content_type="image/gif")
        BotanicalRecord.objects.create(common_name="Tembusu", scientific_name="Cyrtophyllum fragrans",
                                       family="Gentianaceae", image=image,
                                       identification_features="Dark bark", fun_fact="On the $5 note")
        BotanicalRecord.objects.create(common_name="Draft Plant", scientific_name="X", family="Y", image=image,
                                       identification_features="-", fun_fact="-", status=BotanicalRecord.DRAFT)
        items = self.client.get("/api/collectibles").json()["collectibles"]
        self.assertEqual([c["name"] for c in items], ["Tembusu"])
        self.assertEqual(items[0]["category"], "Flora")
        self.assertEqual(items[0]["scientificName"], "Cyrtophyllum fragrans")


class IdentifyTests(ApiTestCase):
    def test_requires_image(self):
        self.assertEqual(self.client.post("/api/identify").status_code, 400)

    @mock.patch.dict("os.environ", {"PLANTNET_API_KEY": "test-key"})
    @mock.patch("portal.api.classifier.available", return_value=False)
    @mock.patch("portal.api.requests.post")
    def test_forwards_plantnet_result(self, fake_post, _no_model):
        fake_post.return_value = mock.Mock(status_code=200, ok=True,
                                           json=lambda: {"results": [{"species": {"scientificNameWithoutAuthor": "Samanea saman"}}]})
        photo = SimpleUploadedFile("plant.jpg", b"jpegbytes", content_type="image/jpeg")
        r = self.client.post("/api/identify", {"image": photo})
        self.assertEqual(r.status_code, 200)
        self.assertEqual(r.json()["results"][0]["species"]["scientificNameWithoutAuthor"], "Samanea saman")
        self.assertEqual(r.json()["source"], "plantnet")

    @mock.patch("portal.api.classifier.available", return_value=True)
    @mock.patch("portal.api.classifier.classify", return_value=[("tanglin_gate", 0.93), ("swan_lake", 0.04)])
    @mock.patch("portal.api.requests.post")
    def test_confident_model_answers_without_plantnet(self, fake_post, _classify, _model):
        photo = SimpleUploadedFile("gate.jpg", b"jpegbytes", content_type="image/jpeg")
        r = self.client.post("/api/identify", {"image": photo}).json()
        fake_post.assert_not_called()
        self.assertEqual(r["source"], "plantdex")
        self.assertEqual(r["results"][0]["species"]["commonNames"], ["Tanglin Gate"])
        self.assertAlmostEqual(r["results"][0]["score"], 0.93)

    @mock.patch.dict("os.environ", {"PLANTNET_API_KEY": "test-key"})
    @mock.patch("portal.api.classifier.available", return_value=True)
    @mock.patch("portal.api.classifier.classify", return_value=[("costus", 0.35), ("calathea", 0.30)])
    @mock.patch("portal.api.requests.post")
    def test_unsure_model_falls_back_to_plantnet(self, fake_post, _classify, _model):
        fake_post.return_value = mock.Mock(status_code=200, ok=True,
                                           json=lambda: {"results": [{"species": {"scientificNameWithoutAuthor": "Samanea saman"}}]})
        photo = SimpleUploadedFile("plant.jpg", b"jpegbytes", content_type="image/jpeg")
        r = self.client.post("/api/identify", {"image": photo}).json()
        fake_post.assert_called_once()
        self.assertEqual(r["source"], "plantnet")

    @mock.patch.dict("os.environ", {}, clear=False)
    @mock.patch("portal.api.classifier.available", return_value=True)
    @mock.patch("portal.api.classifier.classify", return_value=[("costus", 0.35), ("calathea", 0.30)])
    def test_unsure_model_without_plantnet_key_is_no_match(self, _classify, _model):
        import os
        os.environ.pop("PLANTNET_API_KEY", None)
        photo = SimpleUploadedFile("plant.jpg", b"jpegbytes", content_type="image/jpeg")
        r = self.client.post("/api/identify", {"image": photo})
        self.assertEqual(r.status_code, 200)
        self.assertEqual(r.json()["results"], [])
