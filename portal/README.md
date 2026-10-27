# PlantDex Staff Portal

Web portal for **Singapore Botanic Gardens staff** to maintain and monitor the PlantDex
Android app. Built with **Python + Django** (FYP-26-S3-15, CSIT321).

Visitors only use the Android app. This portal is for three staff roles:

| Role | What they do here |
|---|---|
| **User Admin** | Manage staff accounts and profile types (roles and their permissions) |
| **System Admin** | Manage discovery collectibles, read collectible error logs |
| **Researcher** | Maintain botanical and architectural records |

All three can view the **Audit Log**.

> **New to the project?** Read [INSTALLATION.md](INSTALLATION.md) first (15 minutes to a
> running site), then [TODO.md](TODO.md) to see what is done and what to build next.

---

## Quick start (Windows, VS Code terminal)

```powershell
python -m venv venv
venv\Scripts\Activate.ps1          # (cmd.exe: venv\Scripts\activate)
pip install -r requirements.txt
python manage.py migrate
python manage.py seed_roles        # roles, profile types and 3 demo logins
python manage.py seed_demo         # optional: 21 extra test accounts
python manage.py runserver
```

Open <http://127.0.0.1:8000/> and log in:

| Username | Password | Role |
|---|---|---|
| `sysadmin` | `ChangeMe123!` | System Admin |
| `useradmin` | `ChangeMe123!` | User Admin |
| `researcher` | `ChangeMe123!` | Researcher |

These are **development-only** accounts. Never use them on a real server.

Run the automated tests any time (takes about 2 seconds):

```powershell
python manage.py test
```

---

## How the project is organised

```
manage.py
plantdex_portal/          Project settings and root URLs
  settings.py             Config. Reads optional environment variables (see INSTALLATION.md)
  urls.py                 Sends everything to portal/urls.py
portal/                   The one app that holds the whole portal
  roles.py                SECTIONS registry (every page/card) + role and permission helpers
  models.py               AuditLog, Feature, ProfileType
  decorators.py           @feature_required("slug")  <- the access gate for every page
  context_processors.py   Puts `role` into every template (top-bar badge)
  audit.py                log_action(): writes one audit entry
  signals.py              Logs login / logout / failed login automatically
  forms.py                AccountForm, ProfileTypeForm
  views.py                Dashboard, audit log, password-reset confirm
  account_views.py        User Admin: account list / view / edit / suspend
  profile_views.py        User Admin: profile type list / view / edit
  urls.py                 URL table (order matters, see "Gotchas")
  templates/portal/       HTML (base.html is the shared frame)
  templates/403.html, 404.html   Styled error pages
  static/portal/style.css All styling, shared by every page
  management/commands/    seed_roles, seed_demo
  migrations/             Database history. COMMIT THESE.
  tests.py                40 automated tests
```

### The one idea to understand: how access control works

1. Every portal page is a **section** with a slug, such as `accounts` or `audit-logs`.
   Sections are listed in `portal/roles.py` (`SECTIONS`).
2. A **profile type** (User Admin, System Admin, Researcher) is a Django *Group* plus a
   `ProfileType` row listing which sections it may open (stored in the database).
3. Each view is protected by `@feature_required("slug")`. A user gets a **403** unless
   their profile type includes that slug.
4. A User Admin can change those permissions in the portal (*Profile Type Management*)
   and it takes effect on the person's very next click.
5. A user with **no** staff group is treated as a Visitor: they can log in but see no
   cards and get a 403 on every section.
6. Access is always checked on the server. Hiding a button is never enough.

---

## Recipe: adding a new page (for example Botanical Records)

Follow the same steps every time. `account_views.py` is a complete worked example.

1. **Section.** Make sure the slug exists in `SECTIONS` in `roles.py` (the Researcher and
   System Admin ones are already there). Run `python manage.py seed_roles`: it creates the
   feature and grants it to the right default role without touching edited permissions.
2. **Model** in `models.py`, then:
   ```powershell
   python manage.py makemigrations portal
   python manage.py migrate
   ```
   Commit the new file in `portal/migrations/`.
3. **Form** in `forms.py` (a `ModelForm`: Django validates and saves for you).
4. **Views** in a new file such as `botanical_views.py`. Decorate every view with
   `@feature_required("botanical")`. After any create / update / delete call:
   ```python
   from .audit import log_action
   from .models import AuditLog
   log_action(request.user, AuditLog.CREATE, record_type="Botanical Record",
              details=record.common_name, request=request)
   ```
   Use `AuditLog.UPDATE` and `AuditLog.DELETE` for the others. The Audit Log page then
   shows them with no extra work (the *Record type* dropdown fills itself).
5. **URLs** in `portal/urls.py`: add the paths **above** the last line
   `path("section/<slug:slug>/", ...)`, otherwise the placeholder page wins.
6. **Templates** in `templates/portal/`. Start with `{% extends "portal/base.html" %}`.
   Copy `account_list.html` (search + table + paging) and `account_form.html`.
7. **Tests** in `tests.py`: one test for the happy path, one for the 403, one for the
   audit entry.
8. Run `python manage.py test`, then commit.

### Conventions we follow

- Edit and delete actions that change data use **POST**, with `{% csrf_token %}`.
  A GET must never change anything (see the suspend confirmation page).
- Destructive actions have a confirmation page.
- Every change to data writes one audit entry **only if something really changed**.
- Use `messages.success / error / info` for the banners shown after an action.
- Lists are **paginated** and searchable (the PTD sets a 2-second limit for searches).
- Imports inside `portal/` are relative: `from .models import ...` (never
  `from plantdex_portal.portal...`).
- One feature per Git branch, merged by pull request.

---

## Gotchas we already ran into

| Symptom | Cause and fix |
|---|---|
| `/accounts/login/` 404 after opening a page | `LOGIN_URL = 'login'` missing in `settings.py` |
| `TemplateDoesNotExist` | `'portal'` not in `INSTALLED_APPS`, or the file is not in `portal/templates/portal/` (check for a hidden `.txt.txt`) |
| `ModuleNotFoundError: plantdex_portal.portal` | Wrong auto-import. Use `from .models import ...` |
| `Deprecated email settings ... MAILERS` | Django 6.1 replaced `EMAIL_BACKEND` with `MAILERS`. Do not add `EMAIL_BACKEND` |
| A new page shows "under construction" | Its URL is below `section/<slug:slug>/` in `urls.py`. Move it above |
| Pasting code into `manage.py shell` gives `IndentationError` | Paste breaks indentation. Use a management command or script file instead |
| `no such table` / missing column | Run `python manage.py migrate` after every `git pull` |
| CSS changes do not appear | Hard refresh with `Ctrl+Shift+R` |

---

## Known limitations (also listed in TODO.md)

- Password-reset emails print in the terminal (development). A real SMTP service is needed
  for deployment.
- Expired sessions are not written to the audit log (Django cannot detect a silent expiry).
- No rate limiting on login or password reset.
- Demo passwords, `DEBUG`, and the secret key are development settings. See the
  deployment checklist in TODO.md.
- Visitor accounts live in the mobile app's database, not here. The accounts page lists
  any user in *this* database, so visitors only appear if you create test users.

---

## Team

Group FYP-26-S3-15, University of Wollongong / SIM. Supervisor: Dr. Loo Poh Kok.
Repository: <https://github.com/ellyhaniz/botanicvision>
