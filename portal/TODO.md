# PlantDex Staff Portal: Done vs To Do

Status as of **31 Oct 2026**. Story IDs (UA-01, SA-04, RS-09 ...) come from section 13 of
the Preliminary Technical Documentation (PTD).
✅ done and tested  ·  ⬜ to do  ·  ⚠️ needs a team decision

**Progress at a glance**

| Role | Stories | Done | Left |
|---|---|---|---|
| User Admin (UA) | 10 | **10** | 0 |
| Researcher (RS) | 13 | **13** | 0 |
| System Admin (SA) | 9 | **9** | 0 |
| **Total** | **32** | **32** | **0** |

**All 32 user stories are implemented and tested** (177 automated tests). The three
"Log in / Log out / View audit log" stories are shared by all roles. What remains is the
group's open decisions (section 5), connecting the mobile app, and deployment hardening.

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
- 177 automated tests (`python manage.py test`)
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

## 3. System Admin (SA): all stories done ✅

| Story | Feature | Status |
|---|---|---|
| SA-01 / 02 | Log in, log out | ✅ |
| SA-03 | View audit log | ✅ |
| SA-09 | View **collectible error logs**: list, details, filter by type / date / status / collectible / user | ✅ |
| SA-04 | Create discovery collectible (name, description, image, category, rarity, **linked verified record**) | ✅ |
| SA-05 | View collectibles (list and details, linked record, status, unlock count) | ✅ |
| SA-06 | Update collectible (image, category, rarity, linked record) | ✅ |
| SA-07 | Delete collectible, with confirmation and the "collectible in use" rule | ✅ |
| SA-08 | Search and filter collectibles (keyword, category, rarity, status, linked record type) | ✅ |

**What was built for collectibles (and the PTD rules it follows)**
- A collectible links to **exactly one** botanical **or** architecture record. The database
  enforces it (a CHECK constraint), and each record can have **at most one** collectible
  (filtered unique indexes on SQL Server).
- Only **Published (verified)** records can be linked. Draft records are shown in the drop-down
  but refused with a clear message. A record that already has a collectible is refused too
  ("A collectible already exists for this record").
- **Collectible in use:** a collectible that visitors have unlocked cannot be deleted, neither
  from the page nor by the database (`PROTECT`). The System Admin is told to set it to Draft to
  hide it instead. Update pages warn when visitors have already unlocked the card.
- **A Researcher cannot delete a record a collectible depends on.** They are told which
  collectible uses it, and to ask a System Admin to delete or relink it first.
- If a linked record is later deleted or set back to Draft, the collectible still shows, flagged
  **unavailable**, so the System Admin can fix the link.
- Every create, update and delete is written to the audit log (an update lists only the fields
  that changed).
- **Pictures are stored inside the database** (table `portal_storedfile`, served at `/files/...`),
  so everyone who uses the same Azure SQL database sees the same pictures. A replaced or deleted
  collectible removes its old picture, so nothing piles up in the database.
- Default categories **Flora** and **Architecture** are created by migration `0006`.
- `python manage.py seed_collectibles` adds a sample collectible for every Published record,
  plus a few visitor unlocks (so some are "in use"). `--clear` removes them again.

**New tables (migrations 0005 and 0006):** `portal_storedfile`, `portal_collectiblecategory`,
`portal_collectible`, `portal_collectibleunlock`.

**SA-09 notes.** `CollectibleErrorLog` stores the collectible and visitor as **text**. Linking it
to the new `Collectible` table with a nullable foreign key is a possible next step (it would let
an error log open the collectible). Status (Open / Investigating / Resolved) is view-only; no
story covers changing it. `seed_demo` adds 30 sample entries.

### ⚠️ Open questions for SA (answer, then update the PTD)
- [ ] **Delete is a real delete here**, but only for collectibles nobody has unlocked. SA-07
      says "permanently", and the in-use rule protects visitor progress, whereas risk R09
      suggests soft delete. Confirm, then align the document
- [ ] **Rarity levels** are Common, Uncommon, Rare and Hidden. The PUM only shows "Hidden" as
      an example. Confirm the real list
- [ ] **Collectible status** (Draft / Published) is our reading of "current status" in SA-05.
      Confirm that is what the stories mean
- [ ] **`CollectibleUnlock` table.** Not in any user story, but needed for the in-use rule and the
      PTD 7.2 note about the impact on visitors who already unlocked a card. The **mobile API
      must write to it** (visitor username, collectible, time). Agree the exact columns
- [ ] **Category management.** No story lets the System Admin add or rename categories. Only
      Flora and Architecture exist. Add a story if more are needed
- [ ] **Who can see the pictures.** `/files/...` needs a portal login. The mobile app will need
      its own way to fetch card images (for example through the mobile API)
- [ ] ⚠️ **Error-log access conflict.** The access table (7.2) gives collectible error logs to
      *Researchers*, but the user stories (SA-09) and scope give them to *System Admin*. The
      portal follows the user stories
- [ ] ⚠️ **Copy-paste slip in the PTD.** The System Admin WBS rows list "Create / Update /
      Delete an Architecture Record", which belong to Researchers. Replace them with the
      collectible functions
- [ ] The use case for SA-08 is titled "Delete Discovery Collectible". Fix the title

### Known limitations of collectibles
- [ ] Pictures are loaded from the database on every request (with browser caching). That is
      fine for a few dozen cards. For a large collection, move them to Azure Blob Storage
- [ ] No page to restore a deleted collectible (deleting is permanent for unused ones)
- [ ] The picture upload limit is 5 MB. Azure SQL storage is billed, so keep pictures small

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
- [ ] ⚠️ **Record pictures with a shared database.** Botanical and architecture pictures are still
      saved in the local `media/` folder, so teammates see broken images (see INSTALLATION.md
      section 11). **Collectible pictures already live in the database.** The same one-line
      change (`storage=database_storage` on the two record `image` fields, plus a migration)
      would move record pictures too

### Known limitations of the records pages
- [ ] Replaced or deleted images stay on disk (the file is not removed)
- [ ] EXIF data (GPS, camera) is not stripped from uploads (PTD 4.4 asks for it before AI training)
- [ ] Uploaded images are served by Django only in development. Production needs a web server or cloud storage
- [ ] No restore page for soft-deleted records (clear `is_deleted` in `/admin/` or the shell)
- ✅ A Researcher can no longer delete a record that a collectible uses (they are told which one)

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
| 7 | **Where pictures live.** Collectible pictures are stored in the database (Azure SQL). Record pictures are still local files | Decide whether to move record pictures into the database too, or to Azure Blob Storage |
| 8 | **Mobile app and collectibles.** The app must read published collectibles and write `portal_collectibleunlock` and error logs. No mobile API exists yet | Agree the table contracts with whoever builds the API |

---

## 6. Testing and quality

- ✅ 177 automated tests cover login, access control, audit log, accounts, profile types,
  error logs, botanical and architecture records, collectibles, the database picture store,
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

1. **Apply the new migrations to the shared Azure database** (`0005`, `0006`), once, after the
   pull request is merged (see INSTALLATION.md section 11)
2. **Decide where record pictures live** (section 5, item 7) so botanical and architecture pictures
   are also shared
3. **Open questions** in sections 2 to 5, then the polish and deployment lists
4. **Connect the mobile app.** The Android app is still a UI prototype with a hard-coded demo
   login and no networking, and no mobile API exists yet to read collectibles or write unlocks
   and error logs
5. Decide `minSdk` for the Android app (currently 36, Android 16 and newer) against the PTD
   goal of "a wide range of Android smartphones"
6. **Testing evidence for Phase 11:** save the output of `python manage.py test` and screenshots

The PTD puts feature integration in Phase 9 (4 to 17 Oct) and accuracy work in Phase 10,
with the final application due on **14 Nov 2026**.
