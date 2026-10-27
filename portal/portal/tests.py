"""
Automated tests for the PlantDex Staff Portal.

Run with:   python manage.py test
Each test starts from an empty database; setUp() builds the roles and demo users,
so tests never touch your own db.sqlite3.
"""
from datetime import timedelta

from django.contrib.auth.models import Group, User
from django.core import mail
from django.core.management import call_command
from django.test import Client, TestCase
from django.urls import reverse
from django.utils import timezone

from .models import AuditLog, CollectibleErrorLog, Feature, ProfileType

PW = "ChangeMe123!"


def client_for(username):
    """A separate logged-in browser session for the given user."""
    c = Client()
    assert c.login(username=username, password=PW)
    return c


class PortalTestCase(TestCase):
    def setUp(self):
        call_command("seed_roles", verbosity=0)
        self.sysadmin = User.objects.get(username="sysadmin")
        self.useradmin = User.objects.get(username="useradmin")
        self.researcher = User.objects.get(username="researcher")
        # a Visitor-type account: no staff group
        self.visitor = User.objects.create_user("visitor01", "v1@sbg.test", PW,
                                                first_name="Vi", last_name="Sitor")

    def login(self, username):
        self.assertTrue(self.client.login(username=username, password=PW))


# --------------------------------------------------------------------------- login
class LoginTests(PortalTestCase):
    def test_dashboard_requires_login(self):
        r = self.client.get(reverse("dashboard"))
        self.assertRedirects(r, "/login/?next=/")

    def test_login_success_is_audited(self):
        r = self.client.post(reverse("login"), {"username": "researcher", "password": PW})
        self.assertRedirects(r, reverse("dashboard"))
        log = AuditLog.objects.get(action=AuditLog.LOGIN)
        self.assertEqual((log.username, log.role), ("researcher", "Researcher"))

    def test_failed_login_is_audited_without_password(self):
        r = self.client.post(reverse("login"), {"username": "researcher", "password": "wrong"})
        self.assertEqual(r.status_code, 200)
        log = AuditLog.objects.get(action=AuditLog.LOGIN_FAILED)
        self.assertEqual(log.username, "researcher")
        self.assertNotIn("wrong", f"{log.details}{log.username}")

    def test_logout_is_audited(self):
        self.login("researcher")
        self.client.post(reverse("logout"))
        self.assertTrue(AuditLog.objects.filter(action=AuditLog.LOGOUT, username="researcher").exists())

    def test_user_without_role_can_log_in(self):
        """Regression: a Visitor-type account used to crash the login with a 500."""
        r = self.client.post(reverse("login"), {"username": "visitor01", "password": PW})
        self.assertRedirects(r, reverse("dashboard"))
        self.assertEqual(AuditLog.objects.get(action=AuditLog.LOGIN, username="visitor01").role, "")

    def test_suspended_account_cannot_log_in(self):
        self.researcher.is_active = False
        self.researcher.save()
        r = self.client.post(reverse("login"), {"username": "researcher", "password": PW})
        self.assertEqual(r.status_code, 200)
        self.assertNotIn("_auth_user_id", self.client.session)


# --------------------------------------------------------------------------- RBAC
class AccessControlTests(PortalTestCase):
    def test_dashboard_cards_per_role(self):
        expected = {
            "sysadmin": {"collectibles", "error-logs", "audit-logs"},
            "useradmin": {"accounts", "profile-types", "audit-logs"},
            "researcher": {"botanical", "architecture", "audit-logs"},
        }
        for username, slugs in expected.items():
            c = client_for(username)
            cards = {card["slug"] for card in c.get(reverse("dashboard")).context["cards"]}
            self.assertEqual(cards, slugs, username)

    def test_visitor_sees_no_cards(self):
        c = client_for("visitor01")
        self.assertEqual(c.get(reverse("dashboard")).context["cards"], [])

    def test_wrong_role_gets_403(self):
        cases = [
            ("researcher", "/section/accounts/"),
            ("researcher", "/section/profile-types/"),
            ("researcher", "/section/collectibles/"),
            ("sysadmin", "/section/accounts/"),
            ("useradmin", "/section/botanical/"),
            ("visitor01", "/section/audit-logs/"),
            ("visitor01", "/section/accounts/"),
        ]
        for username, url in cases:
            self.assertEqual(client_for(username).get(url).status_code, 403, f"{username} {url}")

    def test_error_pages_are_styled_and_show_correct_role(self):
        c = client_for("researcher")
        r = c.get("/section/accounts/")
        self.assertContains(r, "Access denied", status_code=403)
        self.assertContains(r, "Researcher", status_code=403)          # role badge, not "No role"
        self.assertContains(c.get("/section/nope/"), "Page not found", status_code=404)

    def test_all_staff_can_open_audit_logs(self):
        for username in ("sysadmin", "useradmin", "researcher"):
            self.assertEqual(client_for(username).get(reverse("audit_logs")).status_code, 200, username)

    def test_unknown_section_is_404(self):
        self.login("useradmin")
        self.assertEqual(self.client.get("/section/does-not-exist/").status_code, 404)

    def test_anonymous_is_redirected_from_every_page(self):
        for url in ("/section/accounts/", f"/section/accounts/{self.researcher.pk}/",
                    "/section/profile-types/", "/section/audit-logs/"):
            r = self.client.get(url)
            self.assertEqual(r.status_code, 302, url)
            self.assertIn("/login/", r["Location"])


# --------------------------------------------------------------------------- audit log
class AuditLogTests(PortalTestCase):
    def test_entries_cannot_be_edited_or_deleted(self):
        AuditLog.objects.create(username="x", action=AuditLog.LOGIN)
        entry = AuditLog.objects.first()
        entry.details = "tampered"
        with self.assertRaises(PermissionError):
            entry.save()
        with self.assertRaises(PermissionError):
            entry.delete()

    def test_filters(self):
        AuditLog.objects.create(username="alice", action=AuditLog.CREATE, record_type="Botanical Record")
        AuditLog.objects.create(username="bob", action=AuditLog.UPDATE, record_type="User Account")
        self.login("useradmin")
        url = reverse("audit_logs")

        def names(**params):
            return {entry.username for entry in self.client.get(url, params).context["page"]}

        self.assertEqual(names(user="ali"), {"alice"})
        self.assertEqual(names(action="UPDATE"), {"bob"})
        self.assertEqual(names(record_type="Botanical Record"), {"alice"})
        self.assertEqual(names(user="nobody"), set())

    def test_bad_date_is_ignored_not_a_crash(self):
        self.login("useradmin")
        self.assertEqual(self.client.get(reverse("audit_logs"), {"from": "not-a-date"}).status_code, 200)

    def test_pagination(self):
        for i in range(20):
            AuditLog.objects.create(username=f"u{i}", action=AuditLog.LOGIN)
        self.login("useradmin")
        page = self.client.get(reverse("audit_logs")).context["page"]
        self.assertEqual(len(page), 15)
        self.assertTrue(page.has_next())


# --------------------------------------------------------------------------- error logs (SA-09)
class ErrorLogTests(PortalTestCase):
    def setUp(self):
        super().setUp()
        E = CollectibleErrorLog
        now = timezone.now()
        self.unlock = E.objects.create(
            error_type=E.UNLOCK_FAILED, status=E.OPEN, collectible_name="Tembusu",
            visitor_username="alice", message="Plant not recognised",
            details="confidence 0.41", occurred_at=now - timedelta(days=10))
        self.sync = E.objects.create(
            error_type=E.SYNC_FAILED, status=E.RESOLVED, collectible_name="Burkill Hall",
            visitor_username="bob", message="Sync timed out", occurred_at=now)
        self.login("sysadmin")

    def ids(self, **params):
        return {log.pk for log in self.client.get(reverse("error_logs"), params).context["page"]}

    def test_sysadmin_sees_list_newest_first(self):
        r = self.client.get(reverse("error_logs"))
        self.assertEqual(r.status_code, 200)
        self.assertEqual([log.pk for log in r.context["page"]], [self.sync.pk, self.unlock.pk])
        self.assertContains(r, "Tembusu")

    def test_dashboard_card_opens_the_real_page(self):
        r = self.client.get("/section/error-logs/")
        self.assertTemplateUsed(r, "portal/error_log_list.html")

    def test_other_roles_get_403(self):
        for username in ("researcher", "useradmin", "visitor01"):
            c = client_for(username)
            self.assertEqual(c.get(reverse("error_logs")).status_code, 403, username)
            self.assertEqual(c.get(reverse("error_log_detail", args=[self.unlock.pk])).status_code,
                             403, username)

    def test_filters(self):
        self.assertEqual(self.ids(type="SYNC_FAILED"), {self.sync.pk})
        self.assertEqual(self.ids(status="OPEN"), {self.unlock.pk})
        self.assertEqual(self.ids(collectible="tembu"), {self.unlock.pk})
        self.assertEqual(self.ids(user="bo"), {self.sync.pk})
        today = timezone.localdate().isoformat()
        self.assertEqual(self.ids(**{"from": today}), {self.sync.pk})
        self.assertEqual(self.ids(collectible="nothing"), set())

    def test_empty_message_and_bad_date(self):
        r = self.client.get(reverse("error_logs"), {"collectible": "nothing", "from": "not-a-date"})
        self.assertContains(r, "No error logs found.")

    def test_pagination(self):
        for i in range(20):
            CollectibleErrorLog.objects.create(error_type=CollectibleErrorLog.OTHER, message=f"e{i}")
        page = self.client.get(reverse("error_logs")).context["page"]
        self.assertEqual(len(page), 15)
        self.assertTrue(page.has_next())

    def test_detail(self):
        r = self.client.get(reverse("error_log_detail", args=[self.unlock.pk]))
        self.assertContains(r, "confidence 0.41")
        self.assertContains(r, "Card unlock failed")
        self.assertEqual(self.client.get(reverse("error_log_detail", args=[9999])).status_code, 404)


# --------------------------------------------------------------------------- accounts (UA-07..10)
class AccountManagementTests(PortalTestCase):
    def setUp(self):
        super().setUp()
        self.login("useradmin")

    def test_list_and_search(self):
        r = self.client.get(reverse("accounts"), {"q": "visitor"})
        self.assertEqual([u.username for u in r.context["page"]], ["visitor01"])
        r = self.client.get(reverse("accounts"), {"q": "zzz"})
        self.assertContains(r, "No results found.")

    def test_status_filter(self):
        self.visitor.is_active = False
        self.visitor.save()
        r = self.client.get(reverse("accounts"), {"status": "suspended"})
        self.assertEqual([u.username for u in r.context["page"]], ["visitor01"])

    def test_edit_updates_and_audits(self):
        r = self.client.post(reverse("account_edit", args=[self.visitor.pk]), {
            "first_name": "New", "last_name": "Name", "email": "new@sbg.test", "profile_type": "",
        })
        self.assertRedirects(r, reverse("account_detail", args=[self.visitor.pk]))
        self.visitor.refresh_from_db()
        self.assertEqual((self.visitor.first_name, self.visitor.email), ("New", "new@sbg.test"))
        log = AuditLog.objects.get(record_type="User Account", action=AuditLog.UPDATE)
        self.assertIn("visitor01", log.details)

    def test_promote_visitor_to_researcher_and_back(self):
        group = Group.objects.get(name="Researcher")
        data = {"first_name": "Vi", "last_name": "Sitor", "email": "v1@sbg.test",
                "profile_type": group.pk}
        self.client.post(reverse("account_edit", args=[self.visitor.pk]), data)
        self.assertTrue(self.visitor.groups.filter(name="Researcher").exists())
        data["profile_type"] = ""
        self.client.post(reverse("account_edit", args=[self.visitor.pk]), data)
        self.assertFalse(self.visitor.groups.filter(name="Researcher").exists())

    def test_duplicate_email_rejected(self):
        r = self.client.post(reverse("account_edit", args=[self.visitor.pk]), {
            "first_name": "Vi", "last_name": "Sitor", "email": "researcher@sbg.test",
            "profile_type": ""})
        self.assertEqual(r.status_code, 200)
        self.assertContains(r, "already uses this email")

    def test_required_fields(self):
        r = self.client.post(reverse("account_edit", args=[self.visitor.pk]), {
            "first_name": "", "last_name": "", "email": "", "profile_type": ""})
        self.assertEqual(r.status_code, 200)
        self.assertFalse(AuditLog.objects.filter(record_type="User Account").exists())

    def test_no_change_writes_no_audit_entry(self):
        r = self.client.post(reverse("account_edit", args=[self.visitor.pk]), {
            "first_name": "Vi", "last_name": "Sitor", "email": "v1@sbg.test", "profile_type": ""})
        self.assertEqual(r.status_code, 302)
        self.assertFalse(AuditLog.objects.filter(record_type="User Account").exists())

    def test_own_profile_type_is_locked(self):
        group = Group.objects.get(name="Researcher")
        self.client.post(reverse("account_edit", args=[self.useradmin.pk]), {
            "first_name": "Uma", "last_name": "Admin", "email": "useradmin@sbg.test",
            "profile_type": group.pk})
        self.assertTrue(self.useradmin.groups.filter(name="User Admin").exists())

    def test_suspend_and_reactivate(self):
        url = reverse("account_status", args=[self.visitor.pk])
        self.assertEqual(self.client.get(url).status_code, 200)       # confirmation page only
        self.visitor.refresh_from_db()
        self.assertTrue(self.visitor.is_active)                        # GET changes nothing
        self.client.post(url)
        self.visitor.refresh_from_db()
        self.assertFalse(self.visitor.is_active)
        self.client.post(url)
        self.visitor.refresh_from_db()
        self.assertTrue(self.visitor.is_active)
        self.assertEqual(AuditLog.objects.filter(record_type="User Account").count(), 2)

    def test_cannot_change_own_status(self):
        url = reverse("account_status", args=[self.useradmin.pk])
        self.client.post(url)
        self.useradmin.refresh_from_db()
        self.assertTrue(self.useradmin.is_active)
        self.assertEqual(self.client.get(url).status_code, 302)       # bounced back

    def test_suspended_user_is_logged_out_on_next_request(self):
        other = client_for("researcher")
        self.assertEqual(other.get(reverse("audit_logs")).status_code, 200)
        self.client.post(reverse("account_status", args=[self.researcher.pk]))
        self.assertEqual(other.get(reverse("audit_logs")).status_code, 302)


# --------------------------------------------------------------------------- profile types (UA-04..06)
class ProfileTypeTests(PortalTestCase):
    def setUp(self):
        super().setUp()
        self.login("useradmin")
        self.pt = ProfileType.objects.get(group__name="Researcher")

    def _post(self, pt, description, codes):
        feats = list(Feature.objects.filter(code__in=codes).values_list("pk", flat=True))
        return self.client.post(reverse("profile_edit", args=[pt.pk]),
                                {"description": description, "permissions": feats})

    def test_list_and_search(self):
        r = self.client.get(reverse("profile_types"))
        self.assertEqual(len(r.context["items"]), 3)
        r = self.client.get(reverse("profile_types"), {"q": "zzz"})
        self.assertContains(r, "No results found.")

    def test_update_description_and_permissions_is_audited(self):
        r = self._post(self.pt, "New text", ["botanical", "architecture", "audit-logs", "accounts"])
        self.assertRedirects(r, reverse("profile_detail", args=[self.pt.pk]))
        self.pt.refresh_from_db()
        self.assertEqual(self.pt.description, "New text")
        self.assertTrue(self.pt.permissions.filter(code="accounts").exists())
        log = AuditLog.objects.get(record_type="Profile Type")
        self.assertIn("User Account Management", log.details)

    def test_permission_change_takes_effect_immediately(self):
        """Regression: account pages used to ignore the database permissions."""
        researcher = client_for("researcher")
        self.assertEqual(researcher.get(reverse("accounts")).status_code, 403)
        self._post(self.pt, self.pt.description, ["botanical", "architecture", "audit-logs", "accounts"])
        self.assertEqual(researcher.get(reverse("accounts")).status_code, 200)
        self._post(self.pt, self.pt.description, ["botanical", "architecture", "audit-logs"])
        self.assertEqual(researcher.get(reverse("accounts")).status_code, 403)

    def test_user_admin_cannot_remove_its_own_critical_permissions(self):
        ua = ProfileType.objects.get(group__name="User Admin")
        for dropped in ("profile-types", "accounts"):
            keep = [c for c in ("accounts", "profile-types", "audit-logs") if c != dropped]
            r = self._post(ua, ua.description, keep)
            self.assertEqual(r.status_code, 200, dropped)
            self.assertContains(r, "must keep access")
            self.assertTrue(ua.permissions.filter(code=dropped).exists())

    def test_name_is_not_editable(self):
        form = self.client.get(reverse("profile_edit", args=[self.pt.pk])).context["form"]
        self.assertNotIn("name", form.fields)


# --------------------------------------------------------------------------- password reset + session
class PasswordResetTests(PortalTestCase):
    def test_reset_flow(self):
        r = self.client.post(reverse("password_reset"), {"email": "useradmin@sbg.test"})
        self.assertRedirects(r, reverse("password_reset_done"))
        self.assertEqual(len(mail.outbox), 1)
        link = next(line for line in mail.outbox[0].body.splitlines() if "/reset/" in line)
        r = self.client.get(link[link.index("/reset/"):], follow=True)   # Django swaps the token for a session URL
        self.assertEqual(r.status_code, 200)
        r = self.client.post(r.request["PATH_INFO"],
                             {"new_password1": "Str0ng-New-Pass!9", "new_password2": "Str0ng-New-Pass!9"})
        self.assertRedirects(r, reverse("password_reset_complete"))
        self.assertTrue(self.client.login(username="useradmin", password="Str0ng-New-Pass!9"))
        self.assertTrue(AuditLog.objects.filter(details__contains="password reset").exists())

    def test_unknown_email_gives_same_response_and_sends_nothing(self):
        r = self.client.post(reverse("password_reset"), {"email": "nobody@nowhere.test"})
        self.assertRedirects(r, reverse("password_reset_done"))
        self.assertEqual(len(mail.outbox), 0)

    def test_suspended_account_gets_no_email(self):
        self.researcher.is_active = False
        self.researcher.save()
        self.client.post(reverse("password_reset"), {"email": "researcher@sbg.test"})
        self.assertEqual(len(mail.outbox), 0)

    def test_session_settings(self):
        from django.conf import settings
        self.assertLessEqual(settings.SESSION_COOKIE_AGE, 30 * 60)
        self.assertTrue(settings.SESSION_SAVE_EVERY_REQUEST)


# --------------------------------------------------------------------------- seed commands
class SeedTests(TestCase):
    def test_seed_roles_is_repeatable_and_keeps_edited_permissions(self):
        call_command("seed_roles", verbosity=0)
        pt = ProfileType.objects.get(group__name="Researcher")
        pt.permissions.clear()
        call_command("seed_roles", verbosity=0)
        self.assertEqual(pt.permissions.count(), 0)         # an admin's edit is not overwritten
        self.assertEqual(User.objects.filter(username="researcher").count(), 1)

    def test_new_section_is_granted_to_default_role_only(self):
        from . import roles
        call_command("seed_roles", verbosity=0)
        roles.SECTIONS["demo-new"] = {"title": "Demo New", "desc": "x", "roles": ["Researcher"]}
        try:
            call_command("seed_roles", verbosity=0)
            self.assertTrue(ProfileType.objects.get(group__name="Researcher")
                            .permissions.filter(code="demo-new").exists())
            self.assertFalse(ProfileType.objects.get(group__name="User Admin")
                             .permissions.filter(code="demo-new").exists())
        finally:
            del roles.SECTIONS["demo-new"]

    def test_seed_demo_is_repeatable(self):
        call_command("seed_roles", verbosity=0)
        call_command("seed_demo", verbosity=0)
        before = (User.objects.count(), CollectibleErrorLog.objects.count())
        call_command("seed_demo", verbosity=0)
        self.assertEqual((User.objects.count(), CollectibleErrorLog.objects.count()), before)
        self.assertEqual(before[1], 30)
