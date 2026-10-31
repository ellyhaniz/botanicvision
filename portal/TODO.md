# PlantDex Staff Portal: Done vs To Do

Status as of **28 Oct 2026**. Story IDs (UA-01, SA-04, RS-09 ...) come from section 13 of
the Preliminary Technical Documentation (PTD).
✅ done and tested  ·  ⬜ to do  ·  ⚠️ needs a team decision

**Progress at a glance**

| Role | Stories | Done | Left |
|---|---|---|---|
| User Admin (UA) | 10 | **10** | 0 |
| Researcher (RS) | 13 | **13** | 0 |
| System Admin (SA) | 9 | 4 | 5 |
| **Total** | **32** | **27** | **5** |

The three "Log in / Log out / View audit log" stories are shared by all roles.
**User Admin and Researcher are finished. The only work left is System Admin
collectibles (SA-04 to SA-08).** The Researcher records they must link to now exist.

---|---|---|---|
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
- 113 automated tests (`python manage.py test`)
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
1. ✅ SA-09 error logs: done.
2. Collectibles (SA-04 to SA-08). **The Researcher records now exist**, so a collectible can
   link to a *verified* (Published) `BotanicalRecord` or `ArchitectureRecord`, and must not
   be duplicated for the same object. Copy a `RecordConfig` in `portal/records.py` to get the
   list / create / view / edit / delete / search pages and audit logging almost for free.

**Notes and risks**
- [ ] ⚠️ **Agree the table design with whoever builds the mobile API.** The mobile
      backend writes the unlock failures that SA-09 displays, and reads collectibles.
- [ ] Collectible images can reuse the existing `MEDIA_*` settings and Pillow (already installed)
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

## 4. Researcher (RS): all stories done ✅

| Story | Feature | Status |
|---|---|---|
| RS-01 / 02 | Log in, log out | ✅ |
| RS-03 | View audit log | ✅ (see the scope question in section 1) |
| RS-09 | Create **botanical** record (common name, scientific name, family, image, identification features, fun fact) | ✅ |
| RS-10 | View botanical record | ✅ |
| RS-11 | Update botanical record (image optional: leave empty to keep it) | ✅ |
| RS-12 | Delete botanical record, with a confirmation page | ✅ |
| RS-13 | Search by common name, scientific name or family | ✅ |
| RS-04 | Create **architecture** record (name, image, location, style, construction date, description) | ✅ |
| RS-05 | View architecture record | ✅ |
| RS-06 | Update architecture record | ✅ |
| RS-07 | Delete architecture record, with a confirmation page | ✅ |
| RS-08 | Search by name, location, style or keyword | ✅ |

**What was built (and the PTD rules it follows)**
- Both record types share one code path (`portal/records.py`) and one test suite.
- All required fields enforced, image required on create (PTD 7.4.3). Images must be
  JPG / PNG / WebP and at most 5 MB, and are verified as real images by Pillow.
- **Duplicate warning** while typing the scientific name (botanical) or structure name
  (architecture): it warns and links to the existing record, but never blocks saving.
- **Soft delete** (risk R09): a deleted record is hidden from lists, search and detail pages
  but stays in the database. Deleting asks for confirmation first.
- If a record was removed by someone else while you edit it, you return to the list with a message.
- Paginated (10 per page). An empty search box shows everything.
- Every create, update and delete is in the audit log. An update lists **only the fields that
  changed**, and an edit with no change writes nothing.
- Each record has a **Draft / Published** status (risk R01).
- `python manage.py seed_records` adds 15 sample plants and 15 sample structures.

### ⚠️ Open questions for RS (answer, then update the PTD)
- [ ] **Soft vs hard delete.** RS-07 / RS-12 say "permanently removed", but we followed risk
      R09 (soft delete). Reword the use cases, or tell us to hard-delete
- [ ] **Draft / Published** is not in any use case. Keep it (collectibles must link only to
      *verified* records) and add it to the record data in PTD 4.2
- [ ] **Construction date** is free text (`1868`, `c. 1900`), because heritage dates are often
      approximate. The PTD says "date": confirm
- [ ] **Architecture "fun fact".** The PUM screen has it, the PTD fields do not. It is optional
- [ ] **Researcher audit scope** (see section 1)
- [ ] **Duplicate rule for architecture** is the structure name only
- [ ] **No-JavaScript browsers** get no duplicate warning (everything else still works)
- [ ] ⚠️ **Images with a shared database.** Only the picture path is stored in the database,
      the file stays on the uploader's computer (see INSTALLATION.md section 11). Choose:
      Azure Blob Storage, a shared folder, or one demo machine

### Known limitations of the records pages
- [ ] Replaced or deleted images stay on disk (the file is not removed)
- [ ] EXIF data (GPS, camera) is not stripped from uploads (PTD 4.4 asks for it before AI training)
- [ ] Uploaded images are served by Django only in development. Production needs a web server or cloud storage
- [ ] No restore page for soft-deleted records (clear `is_deleted` in `/admin/` or the shell)
- [ ] Researchers cannot yet see which collectibles use a record before deleting it. Add that check with SA-04

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

- ✅ 113 automated tests cover login, access control, audit log, accounts, profile types,
  error logs, botanical and architecture records,
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

1. **System Admin: Collectibles** (SA-04 to SA-08), linking to Published records. Reuse
   `portal/records.py`. Add the nullable `collectible` foreign key to `CollectibleErrorLog`
2. **Decide how images are shared** (section 4) before any shared demo
3. **Open questions** in sections 2 to 5, then the polish and deployment lists
4. **Connect the mobile app.** The Android app is still a UI prototype with a hard-coded
   demo login and no networking, and no mobile API exists yet to write error logs or read records
5. Decide `minSdk` for the Android app (currently 36, Android 16 and newer) against the PTD
   goal of "a wide range of Android smartphones"

The PTD puts feature integration in Phase 9 (4 to 17 Oct) and accuracy work in Phase 10,
with the final application due on **14 Nov 2026**.
