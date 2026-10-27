# Installation Guide

Get the PlantDex Staff Portal running on your own computer. Allow about 15 minutes.
Written for **Windows + VS Code**. Mac / Linux differences are noted where they exist.

---

## 1. What you need installed

| Tool | Version | Check with | Get it from |
|---|---|---|---|
| **Python** | **3.12 or newer** (the team uses 3.14) | `python --version` | python.org (tick *Add python.exe to PATH*) |
| **Git** | any recent | `git --version` | git-scm.com |
| **VS Code** | any recent | | code.visualstudio.com |
| VS Code extension **Python** (Microsoft) | | | Extensions panel |

Django 6.1 needs Python 3.12 or newer. Python 3.11 or older will **not** work.

You do **not** need XAMPP, Apache, or a database server. The portal runs on Django's
own development server and stores data in a local SQLite file.

---

## 2. Get the code

```powershell
git clone https://github.com/ellyhaniz/botanicvision.git
cd botanicvision
```

> If the project sits in a sub-folder of the repository, `cd` into the folder that
> contains `manage.py`. Every command below is run from that folder.

Open the folder in VS Code: **File > Open Folder**, or run `code .`

---

## 3. Create a virtual environment

A virtual environment keeps this project's packages separate from the rest of your
computer. Open the VS Code terminal with `` Ctrl+` `` and run:

```powershell
python -m venv venv
venv\Scripts\Activate.ps1
```

Your prompt should now start with `(venv)`.

- **Command Prompt (cmd.exe)** instead of PowerShell: `venv\Scripts\activate`
- **Mac / Linux:** `source venv/bin/activate`
- **PowerShell says "running scripts is disabled"?** Run this once, then try again:
  ```powershell
  Set-ExecutionPolicy -Scope CurrentUser -ExecutionPolicy RemoteSigned
  ```

Tell VS Code to use it: press `Ctrl+Shift+P`, choose **Python: Select Interpreter**, and
pick the one that shows `venv`.

**Every time you open a new terminal, activate the venv again.**

---

## 4. Install the packages

```powershell
pip install -r requirements.txt
```

Check it worked:

```powershell
python -m django --version
```

You should see `6.1.x`.

---

## 5. Build your database

Each person has their **own private database** (a file called `db.sqlite3`). It is not
stored in Git, so you build it from the code:

```powershell
python manage.py migrate
python manage.py seed_roles
python manage.py seed_demo
```

| Command | What it does |
|---|---|
| `migrate` | Creates all the tables |
| `seed_roles` | Creates the three roles, their permissions, and 3 demo logins |
| `seed_demo` | *(optional)* Adds 21 test accounts: 12 researchers, 8 visitors, 1 suspended staff. Handy for trying search, paging and suspend |

All three are safe to run again. They skip anything that already exists and never
overwrite permissions that were edited in the portal.

---

## 6. Run it

```powershell
python manage.py runserver
```

Open <http://127.0.0.1:8000/>. You will be sent to the login page.

| Username | Password | Role | You should see |
|---|---|---|---|
| `sysadmin` | `ChangeMe123!` | System Admin | Collectible Management, Error Logs, Audit Logs |
| `useradmin` | `ChangeMe123!` | User Admin | User Account Management, Profile Type Management, Audit Logs |
| `researcher` | `ChangeMe123!` | Researcher | Botanical Records, Architecture Records, Audit Logs |

Stop the server with `Ctrl+C`.

### Quick check that everything works

- [ ] Log in as `useradmin`, open **User Account Management**, and search for `staff`.
- [ ] Open **Audit Logs**. Your login should be at the top.
- [ ] Log out. Log in as `researcher` and type `/section/accounts/` in the address bar.
      You should get **403 Forbidden**.
- [ ] On the login page click **Forgot your password?**, enter `useradmin@sbg.test`.
      Look at the terminal running the server: the reset email is printed there.
- [ ] Run `python manage.py test`. It should end with `OK`.

If any of these fail, see section 9.

---

## 6b. Optional: a Django superuser

The three demo accounts are enough for normal work. A superuser also unlocks Django's
built-in `/admin/` site and bypasses all permission checks, which is useful as an
emergency way back in if someone edits profile permissions badly:

```powershell
python manage.py createsuperuser
```

---

## 7. Everyday workflow with Git

Start of each work session:

```powershell
git pull
pip install -r requirements.txt     # only matters if requirements.txt changed
python manage.py migrate            # ALWAYS: applies teammates' database changes
```

Work on your own branch, one feature per branch:

```powershell
git checkout -b feature/botanical-records
# ... code ...
python manage.py test
git add .
git commit -m "Add botanical record create and list"
git push -u origin feature/botanical-records
```

Then open a pull request on GitHub for a teammate to review.

**Rules that prevent most problems**

1. **Always commit migration files** (`portal/migrations/000X_*.py`). Teammates cannot
   build the tables without them.
2. **Never commit** `db.sqlite3`, `venv/`, or passwords. `.gitignore` already excludes them.
3. After changing `models.py`: `python manage.py makemigrations portal`, then `migrate`,
   then commit the new migration file and tell the team.
4. **Two people changed models at the same time?** Django reports a migration conflict
   after merging. Fix with `python manage.py makemigrations --merge`, then commit.
5. Before pushing, run `python manage.py test`.

### Start over with a clean database

If your local data is messy, stop the server, delete `db.sqlite3`, and rebuild:

```powershell
del db.sqlite3
python manage.py migrate
python manage.py seed_roles
python manage.py seed_demo
```

---

## 8. Optional: use Microsoft SQL Server instead of SQLite

The project documents name Microsoft SQL Server. The portal supports it through
environment variables, with **no code changes**. **Do this only after the team agrees on
one database**, and ideally on a separate Git branch first.

> **Status:** the SQL Server path is written against the official `mssql-django`
> documentation, which lists Django 6.1 and Python 3.14 as supported. It has **not** been
> tested end-to-end by us yet. Try it on a branch and report problems.

### 8.1 Install

1. **Microsoft ODBC Driver 18 for SQL Server** (download from Microsoft).
2. **SQL Server** or **SQL Server Express**, plus **SSMS** to manage it. Or use an
   Azure SQL Database.
3. In SSMS, create an **empty database** called `PlantDexDB`.
4. In your venv:
   ```powershell
   pip install -r requirements-mssql.txt
   python -c "import pyodbc; print(pyodbc.drivers())"
   ```
   The list should contain `ODBC Driver 18 for SQL Server`.

### 8.2 Tell Django to use it

The settings read **environment variables**. They do not read a `.env` file. Set them in
the same terminal you use to run Django (they last until you close the terminal):

**Local SQL Server Express with Windows login**

```powershell
$env:DB_ENGINE  = "mssql"
$env:DB_HOST    = "localhost\SQLEXPRESS"
$env:DB_NAME    = "PlantDexDB"
$env:DB_TRUSTED = "1"
```

**SQL Server login or Azure SQL**

```powershell
$env:DB_ENGINE   = "mssql"
$env:DB_HOST     = "yourserver.database.windows.net"
$env:DB_NAME     = "PlantDexDB"
$env:DB_USER     = "your_user"
$env:DB_PASSWORD = "your_password"
```

Optional: `DB_PORT`, and `DB_DRIVER` if you use ODBC Driver 17.

Then build the tables in SQL Server:

```powershell
python manage.py migrate
python manage.py seed_roles
python manage.py runserver
```

To go back to SQLite, close the terminal (or run `Remove-Item Env:DB_ENGINE`).

### 8.3 If the whole team shares one SQL Server

- Everyone must be able to reach it over the network (for Azure, add each person's IP to
  the firewall).
- **One person** runs `migrate`. Everyone else only pulls the code.
- Do not share passwords in Git, chat, or screenshots. Send them privately.
- Shared data means shared mistakes: do not delete rows others are using.

### 8.4 Django 6.1 limits on SQL Server

The database-level delete options (`DB_CASCADE`, `DB_SET_NULL`, `DB_SET_DEFAULT`) and the
bitwise aggregates (`BitAnd`, `BitOr`, `BitXor`) are not supported by this backend. Use
the normal `on_delete=models.CASCADE` / `models.SET_NULL` in models, as we already do.

---

## 9. Troubleshooting

| What you see | Fix |
|---|---|
| `python` is not recognised | Reinstall Python and tick *Add to PATH*, or try `py` instead of `python` |
| `No module named django` | The venv is not active. Run `venv\Scripts\Activate.ps1`, then `pip install -r requirements.txt` |
| Packages were installed globally by mistake | Check the prompt starts with `(venv)` **before** running `pip install` |
| `Django 6.1 requires Python 3.12` | Install a newer Python and recreate the venv |
| `no such table: ...` | Run `python manage.py migrate` |
| Login fails for demo users | Run `python manage.py seed_roles` (an existing user keeps their old password, so if you changed it, use that) |
| Login page is unstyled | Hard refresh with `Ctrl+Shift+R` |
| `/accounts/login/` 404 | `LOGIN_URL = 'login'` is missing from `settings.py` |
| `TemplateDoesNotExist` | `'portal'` missing from `INSTALLED_APPS`, or the file is in the wrong folder or has a hidden `.txt.txt` extension |
| `Deprecated email settings are not allowed when MAILERS is defined` | Remove any `EMAIL_BACKEND` line. Django 6.1 uses `MAILERS` |
| `Can't open lib 'ODBC Driver 18 for SQL Server'` | The ODBC driver is not installed, or `DB_DRIVER` does not match `pyodbc.drivers()` |
| `Port is already in use` | Another `runserver` is open. Close it, or use `python manage.py runserver 8001` |
| Migration conflict after merging | `python manage.py makemigrations --merge` |
| No reset email appears | It prints in the **terminal running the server**, and only if the account has an email and is active |

Still stuck? Copy the **last 15 lines** of the error from the terminal (not a photo of
the browser page) and ask in the team chat.

---

## 10. Before any real deployment (not needed for development)

- [ ] Set a real secret key: `$env:DJANGO_SECRET_KEY = "<long random string>"`
- [ ] `$env:DJANGO_DEBUG = "0"` and `$env:DJANGO_ALLOWED_HOSTS = "your.domain"`
- [ ] Delete or change every demo password (`ChangeMe123!`)
- [ ] Do not run `seed_demo`
- [ ] Replace the console email setup in `MAILERS` (settings.py) with real SMTP
- [ ] Serve over HTTPS and run behind a production server (not `runserver`)
