# PlantDex team guide

How to get the latest code, run the portal, and use the app on your phone.

> ## ⚠️ Do NOT commit or push to `main`
> `main` is the shared, working version. Work on **your own branch** and open a **pull
> request**. Elly reviews and merges it. See [Saving your work](#5-saving-your-work-branches-not-main).

---

## 1. One-time setup

Install these once:

| What | Why |
|---|---|
| **Git** | to pull the code |
| **Python 3.13** | runs the portal |
| **Android Studio** | builds the app |
| **Microsoft ODBC Driver 18 for SQL Server** | lets the portal reach the Azure database |

Then:

1. **Get the code** (skip if you already have the folder):
   ```powershell
   git clone https://github.com/ellyhaniz/botanicvision.git
   cd botanicvision
   ```
2. **Set up the portal's Python environment:**
   ```powershell
   cd portal
   python -m venv venv
   .\venv\Scripts\Activate.ps1
   pip install -r requirements-mssql.txt
   ```
3. **Ask Elly for two things** (send them privately, never in a group chat or a file):
   - to **add your IP address** to the Azure firewall. Find it by searching "what is my IP".
     Your IP changes on a different Wi-Fi, so ask again if the portal stops connecting.
   - the **Azure database login and password**.

---

## 2. Every time: get the latest code

From the project folder:

```powershell
git switch main
git pull origin main
```

If `pip` packages changed (`requirements*.txt`), run this too:

```powershell
cd portal
.\venv\Scripts\Activate.ps1
pip install -r requirements-mssql.txt
```

---

## 3. Launch the portal

In PowerShell, from the `portal` folder:

```powershell
powershell -ExecutionPolicy Bypass -File .\start_portal.ps1
```

- Enter the **Azure login** and **password** when asked.
- Wait for `Starting development server at http://127.0.0.1:8000/`.
- Open **http://localhost:8000** in your browser.
- **Keep that window open.** Closing it stops the portal (and the app can't reach the server).

**The first request after a quiet spell can take up to a minute.** The Azure database
pauses itself when nobody uses it and needs time to wake up. Just wait and try again.

### Log in to the portal

Portal accounts are **staff accounts** (Researcher or System Admin). Ask Elly (System
Admin) to create one for you. Your **app** account will not work here.

### ⚠️ Shared database rules

Everyone uses the **same Azure database**, so:

- **Don't run `migrate`, `seed_…`, or anything with `--clear`** against Azure. Elly does
  database changes, so nobody wipes or breaks everyone's data.
- **Don't add new models / migrations** without checking with Elly first.

---

## 4. Run the app on your phone

1. On the phone: **Settings → About phone → tap "Build number" 7 times** to unlock
   Developer options, then **Developer options → turn on USB debugging**.
2. Plug the phone in by USB and tap **Allow** on the phone.
3. Open the project folder in **Android Studio**, pick your phone at the top, press **Run ▶**.
4. **Let the phone reach your portal.** In Android Studio's **Terminal**, run:
   ```powershell
   & "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" reverse tcp:8000 tcp:8000
   ```
   Do this again **every time you re-plug the phone**. Without it, the app says
   "Couldn't reach the server".
5. The portal (step 3) must be running while you use the app.

### Log in to the app

On the app's login screen, tap **Register** to make a visitor account, then log in with it.
(App accounts are visitors, separate from portal staff accounts.)

**Things to try:** scan one of the 13 plants or landmarks (e.g. Tanglin Gate, Golden
Trumpet, Peace Lily) to unlock its card, then check **Inventory**, the **home screen**,
**Notifications** (bell) and **Share** on a card.

---

## 5. Saving your work: branches, not `main`

**Before you change anything**, make your own branch:

```powershell
git switch main
git pull origin main
git switch -c yourname/what-you-are-doing      # e.g. aisha/researcher-search
```

Save and upload your work:

```powershell
git add <the files you changed>
git commit -m "Short description of the change"
git push -u origin yourname/what-you-are-doing
```

Then on GitHub, open a **pull request** from your branch into `main` and tell Elly.
**Don't merge it yourself.**

### Oops, I committed on `main`

Don't push. Move the commit onto a branch instead:

```powershell
git switch -c yourname/my-fix        # your commit comes with you
git switch main
git reset --hard origin/main         # main goes back to the shared version
git switch yourname/my-fix
```

Then push the branch and open a pull request as above. If you already **pushed** to
`main`, tell Elly straight away instead of trying to fix it.

---

## Troubleshooting

| Problem | Fix |
|---|---|
| App: "Couldn't reach the server" | Is the portal window still running? Re-run the `adb reverse` command from step 4. |
| Portal: "Login failed for user" | Wrong Azure login or password. Run the script again. |
| Portal: "Server is not found" or a timeout | Wait a minute (the database is waking up) and try again. If it keeps failing, your IP probably changed: ask Elly to add it to the firewall. |
| "running scripts is disabled on this system" | Use the full `powershell -ExecutionPolicy Bypass -File .\start_portal.ps1` command. |
| `No module named django` / `mssql` | Activate the venv (`.\venv\Scripts\Activate.ps1`) and run `pip install -r requirements-mssql.txt`. |
| Port 8000 already in use | The portal is already running in another window. Use that one. |
| The app on the phone looks old | Pull the latest `main`, then press Run in Android Studio again. |
| Plant photos in the portal show as broken | Record photos are saved on the PC that uploaded them, so they're missing on other PCs. This is expected for now. |
