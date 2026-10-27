# PlantDex Staff Portal: Done vs To Do

Status as of **28 Oct 2026**. Story IDs (UA-01, SA-04, RS-09 ...) come from section 13 of
the Preliminary Technical Documentation (PTD).
✅ done and tested  ·  ⬜ to do  ·  ⚠️ needs a team decision

**Progress at a glance**

| Role | Stories | Done | Left |
|---|---|---|---|
| User Admin (UA) | 10 | **10** | 0 |
| System Admin (SA) | 9 | 4 | 5 |
| Researcher (RS) | 13 | 3 | 10 |
| **Total** | **32** | **17** | **15** |

The three "Log in / Log out / View audit log" stories are shared by all roles, so the
real remaining work is **15 stories**: 5 for System Admin, 10 for Researcher.

---

## 1. Shared by all roles

### ✅ Done
- Role-aware login and dashboard (cards depend on the profile type)
- Log out, with a button on every page
- Wrong-password message, and suspended accounts blocked from logging in
- **Server-side access control** (403 if the profile type lacks the section), including
  a styled 403 and 404 page
- Login, logout and failed login are written to the audit log automatically
- **Audit log page** (UA-03 / SA-03 / RS-03): read-only, newest first, filters for user,
  action, **record type** and date range, pagination, and the "No audit log entries
  found" message
- **Forgot / reset password** (emailed one-hour link, used once, logged to the audit log).
  Emails print in the terminal during development
- **Session timeout:** 15 minutes of inactivity, plus a notice on the login page
- 40 automated tests (`python manage.py test`)
- Setup for teammates: `README.md`, `INSTALLATION.md`, `requirements.txt`, seed commands

### ⬜ To do
- [ ] **Expired-session audit entry.** UA-02 says the system records an expired-session
      logout. Django cannot see a silent expiry. Either write a small middleware, or
      soften that sentence in the PTD
- [ ] **Rate limiting** on login and password reset (brute-force protection)
- [ ] **Real SMTP email** for password reset in deployment (currently console only)
- [ ] ⚠️ **Researcher audit scope.** Researchers currently see *all* entries. The PTD says
      they review "actions on architecture and botanical records". Decide, then filter
      `AuditLog` by `record_type` for that role
- [ ] Dashboard polish (icons, welcome banner, quick stats), optional

---

## 2. User Admin (UA): all stories done ✅

| Story | Feature | Status |
|---|---|---|
| UA-01 / 02 | Log in, log out | ✅ |
| UA-03 | View audit log | ✅ |
| UA-07 | View user accounts (list and details) | ✅ |
| UA-09 | Search accounts by name, username or email, plus a status filter | ✅ |
| UA-08 | Update account (name, email, profile type), with validation and a duplicate-email check | ✅ |
| UA-10 | Suspend / reactivate with a confirmation page. **A User Admin cannot change their own status or profile type** | ✅ |
| UA-04 | View profile types (description, permissions, number of users) | ✅ |
| UA-06 | Search profile types | ✅ |
| UA-05 | Update description and permissions. Takes effect on the user's next click | ✅ |

Safeguard: the *User Admin* profile type cannot lose **User Account Management** or
**Profile Type Management**, so nobody can lock the portal by accident.

### ⚠️ Open questions for UA (answer, then update the PTD)
- [ ] **Profile status.** Section 4.2 lists "profile status" for profile types, but no
      use case defines it. Add an Active/Inactive field, or remove it from the document
- [ ] **Creating staff accounts.** No UA story covers it, so accounts are created with
      `seed_roles` or Django `/admin/`. Add a "Create account" story, or confirm this
      is acceptable
- [ ] **Escalation.** A User Admin can currently promote any account to System Admin and
      edit a superuser. Decide whether to restrict that
- [ ] **Wireframes.** Compare *User Account Management*, *Profile Type Management* and
      the two *Edit* screens against the wireframes in the PTD, and adjust the layout
- [ ] **Visitors.** Visitor accounts belong to the mobile app's database. Decide whether
      the portal should list them at all (right now it shows any user in its own database)

---

## 3. System Admin (SA)

| Story | Feature | Status |
|---|---|---|
| SA-01 / 02 | Log in, log out | ✅ |
| SA-03 | View audit log | ✅ |
| SA-09 | View **collectible error logs**: list, details, filter by type / date / status / collectible / user | ✅ |
| SA-04 | Create discovery collectible (name, description, image, category, rarity, **linked verified object**) | ⬜ |
| SA-05 | View collectibles (list and details, linked record, status) | ⬜ |
| SA-06 | Update collectible (image, category, linked object) | ⬜ |
| SA-07 | Delete collectible, with confirmation and an "in use" check | ⬜ |
| SA-08 | Search and filter collectibles (keyword, category, linked object, status) | ⬜ |

**Needs new models:** `CollectibleCategory`, `Collectible`. (`CollectibleErrorLog` is done.)
(Section 4.2 of the PTD already lists the fields for each.)

**SA-09 notes.** `CollectibleErrorLog` stores the collectible and visitor as text, because
`Collectible` does not exist yet and visitors live with the mobile app. When `Collectible` is
built, add a nullable `collectible` foreign key. Check the fields against PTD 4.2. Status
(Open / Investigating / Resolved) is view-only for now; no story covers changing it.
`seed_demo` adds 30 sample entries, and test rows can be added in Django `/admin/`.

**Order of work**
1. **SA-09 error logs first.** It does not depend on anything else.
2. Collectibles (SA-04 to SA-08) **after** the Researcher records exist, because a
   collectible must link to a *verified* botanical or architectural record, and must not
   be duplicated for the same object.

**Notes and risks**
- [ ] ⚠️ **Agree the table design with whoever builds the mobile API.** The mobile
      backend writes the unlock failures that SA-09 displays, and reads collectibles.
- [ ] Image upload needs `pip install Pillow`, plus `MEDIA_ROOT` and `MEDIA_URL`
- [ ] ⚠️ **Error-log access conflict.** The access table (7.2) gives collectible error
      logs to *Researchers*, but the user stories (SA-09) and scope give them to
      *System Admin*. The portal follows the user stories
- [ ] ⚠️ **Copy-paste slip in the PTD.** The System Admin WBS rows list "Create / Update /
      Delete an Architecture Record", which belong to Researchers. Replace them with
      the collectible functions
- [ ] Before updating or deleting a collectible, show the impact on visitors who already
      unlocked it (the PTD asks for this in 7.2)
- [ ] The use case for SA-08 is titled "Delete Discovery Collectible". Fix the title

---

## 4. Researcher (RS)

| Story | Feature | Status |
|---|---|---|
| RS-01 / 02 | Log in, log out | ✅ |
| RS-03 | View audit log | ✅ (see the scope question in section 1) |
| RS-09 | Create **botanical** record (common name, scientific name, family, image, identification features, fun fact) | ⬜ |
| RS-10 | View botanical record | ⬜ |
| RS-11 | Update botanical record | ⬜ |
| RS-12 | Delete botanical record, with confirmation | ⬜ |
| RS-13 | Search botanical records by common name, scientific name or family | ⬜ |
| RS-04 | Create **architecture** record (name, image, location, style, construction date, description) | ⬜ |
| RS-05 | View architecture record | ⬜ |
| RS-06 | Update architecture record | ⬜ |
| RS-07 | Delete architecture record, with confirmation | ⬜ |
| RS-08 | Search architecture records by name, location or keyword | ⬜ |

**Needs new models:** `BotanicalRecord` and `ArchitectureRecord`.

**Validation rules from the PTD (7.4.3)**
- All required fields present and valid
- Warn (and allow cancel or continue) if a **scientific name** already exists (botanical),
  or the structure may already exist (architecture)

**Order of work:** build **Botanical Records first** (RS-09 to RS-13). It teaches the
whole create / view / update / delete / search pattern. Architecture is then a near copy.

**Notes and risks**
- [ ] ⚠️ **Hard delete or soft delete?** RS-07 and RS-12 say "permanently removed", but
      risk R09 recommends a soft delete (`is_deleted` flag) because collectibles link to
      these records. We suggest soft delete, then update the use cases
- [ ] ⚠️ **Draft vs Published status** (risk R01) so unverified records are not shown to
      visitors. Collectibles may only link to *verified* records
- [ ] Image upload: `pip install Pillow`, `MEDIA_ROOT`, `MEDIA_URL`. Check file type and
      size (PTD 7.4.3). Add `media/` serving for development only
- [ ] Every create, update and delete calls `log_action(...)` (see the recipe in README)
- [ ] Add indexes on the searched columns (risk R05, 2-second search limit)
- [ ] The reference images may later feed the AI training set, so agree the storage
      location with whoever trains the model

---

## 5. Decisions to confirm as a group

| # | Question | Why it matters |
|---|---|---|
| 1 | **Web portal vs mobile app for staff.** Your PTD and PUM describe staff screens (for example *Create Collectible (Botanical)*) as part of the Android app. This Django portal is a separate website | Update the architecture diagram, scope statement and PUM, and tell Dr. Loo Poh Kok |
| 2 | **Backend framework.** The PTD says Flask in some places and FastAPI in others. The mobile API and this Django portal are two different servers | One consistent description in the documents |
| 3 | **Database.** SQLite per person for now (works today). SQL Server is supported but untested (see INSTALLATION.md section 8) | Choose one shared server before integration with the mobile API |
| 4 | **Does the portal share `PlantDexDB` with the mobile API?** Django creates its own `auth_*` and `portal_*` tables | Avoid two sources of truth for users |
| 5 | **Session timeout value.** We chose 15 minutes | Write it into PTD section 8.2 |
| 6 | **Permission meaning.** A "permission" is access to one portal section (page group) | Describe this in the report so it does not look unspecified |

---

## 6. Testing and quality

- ✅ 40 automated tests cover login, access control, audit log, accounts, profile types,
  password reset and the seed commands
- [ ] Add tests alongside every new feature (happy path, 403, audit entry)
- [ ] Manual test pass on a real phone-sized browser window (the layout is responsive,
      but untested on devices)
- [ ] Phase 11 testing evidence: save the `python manage.py test` output and screenshots

---

## 7. Deployment checklist (later, not needed for development)

- [ ] `DJANGO_SECRET_KEY` set to a long random value
- [ ] `DJANGO_DEBUG=0` and `DJANGO_ALLOWED_HOSTS` set
- [ ] All `ChangeMe123!` passwords changed or accounts removed, and `seed_demo` not run
- [ ] Real SMTP in `MAILERS`
- [ ] HTTPS, and a production server instead of `runserver`
- [ ] Database backups, and a tested restore (risk R09)
- [ ] Remove Django `/admin/` from the public site, or restrict it

---

## 8. Suggested order for the team

1. **Researcher: Botanical Records** (RS-09 to RS-13). Teaches the pattern for everyone
2. **System Admin: Error Logs** (SA-09). Independent, so someone can do it in parallel
3. **Researcher: Architecture Records** (RS-04 to RS-08). Reuse the botanical code
4. **System Admin: Collectibles** (SA-04 to SA-08). Needs steps 1 and 3
5. **Open questions** in sections 2 to 5, then the polish and deployment lists

Suggested split: one person on Botanical, one on Error Logs, then Architecture and
Collectibles. Each person works on their own branch and adds tests with their feature.
The timeline in the PTD puts feature integration in Phase 9 (4 to 17 Oct) and accuracy
work in Phase 10, with the final application due on **14 Nov 2026**.
