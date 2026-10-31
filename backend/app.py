import os
from datetime import datetime, timedelta, timezone

import requests
from flask import Flask, jsonify, request
from werkzeug.security import check_password_hash, generate_password_hash

from db import get_connection

app = Flask(__name__)

DEFAULT_VISITOR_PROFILE_TYPE_ID = 4


def row_to_profile_type(row):
    return {
        "id": row.ProfileTypeId,
        "roleCode": row.RoleCode,
        "name": row.ProfileTypeName,
        "description": row.Description,
        "permissions": row.Permissions,
    }


USER_JOIN_QUERY = (
    "SELECT u.UserId, u.Username, u.FullName, u.Email, u.AccountStatus, u.CreatedDatetime, "
    "       p.ProfileTypeId, p.RoleCode, p.ProfileTypeName "
    "FROM Users u JOIN ProfileTypes p ON p.ProfileTypeId = u.ProfileTypeId "
)


def row_to_user(row):
    return {
        "userId": row.UserId,
        "username": row.Username,
        "fullName": row.FullName,
        "email": row.Email,
        "accountStatus": row.AccountStatus,
        "createdDatetime": row.CreatedDatetime.strftime("%d %b %Y"),
        "profileTypeId": row.ProfileTypeId,
        "role": row.RoleCode,
        "profileTypeName": row.ProfileTypeName,
    }


def log_audit(cur, user_id, action, affected_record, details):
    cur.execute(
        "INSERT INTO AuditLog (UserId, ActionPerformed, AffectedRecord, Details) VALUES (?, ?, ?, ?)",
        user_id, action, affected_record, details,
    )


# ---------------------------------------------------------------- auth --

@app.post("/api/auth/register")
def register():
    """Public self-registration — always creates a Visitor account.

    Privileged roles (Researcher, User Admin, System Admin) can only be
    created by a User Admin via /api/admin/create-user.
    """
    data = request.get_json(silent=True) or {}
    email = (data.get("email") or "").strip()
    full_name = (data.get("fullName") or "").strip()
    username = (data.get("username") or "").strip()
    password = data.get("password") or ""

    if not all([email, full_name, username, password]):
        return jsonify(message="Please fill in every field"), 400

    conn = get_connection()
    try:
        cur = conn.cursor()
        cur.execute("SELECT 1 FROM Users WHERE Username = ?", username)
        if cur.fetchone():
            return jsonify(message="That username is already taken"), 409

        password_hash = generate_password_hash(password)
        cur.execute(
            "INSERT INTO Users (ProfileTypeId, Username, FullName, Email, PasswordHash, AccountStatus) "
            "OUTPUT INSERTED.UserId "
            "VALUES (?, ?, ?, ?, ?, 'ACTIVE')",
            DEFAULT_VISITOR_PROFILE_TYPE_ID, username, full_name, email, password_hash,
        )
        new_user_id = cur.fetchone()[0]
        log_audit(cur, new_user_id, "REGISTERED", None, f"{username} registered a Visitor account")
        conn.commit()
        return jsonify(message="Account created"), 201
    finally:
        conn.close()


@app.post("/api/auth/login")
def login():
    data = request.get_json(silent=True) or {}
    username = (data.get("username") or "").strip()
    password = data.get("password") or ""

    if not username or not password:
        return jsonify(message="Username and password are required"), 400

    conn = get_connection()
    try:
        cur = conn.cursor()
        cur.execute(
            "SELECT u.UserId, u.PasswordHash, u.Email, u.FullName, u.AccountStatus, u.CreatedDatetime, "
            "       p.ProfileTypeId, p.RoleCode, p.ProfileTypeName "
            "FROM Users u JOIN ProfileTypes p ON p.ProfileTypeId = u.ProfileTypeId "
            "WHERE u.Username = ?",
            username,
        )
        row = cur.fetchone()
        if not row or not check_password_hash(row.PasswordHash, password):
            return jsonify(message="Invalid username or password"), 401

        if row.AccountStatus != "ACTIVE":
            return jsonify(message="This account has been deactivated. Contact a User Admin."), 403

        cur.execute("INSERT INTO Sessions (UserId) OUTPUT INSERTED.SessionId VALUES (?)", row.UserId)
        session_id = cur.fetchone()[0]
        log_audit(cur, row.UserId, "LOGIN", None, f"{username} signed in as {row.ProfileTypeName}")
        conn.commit()
        return jsonify(
            userId=row.UserId,
            sessionId=session_id,
            username=username,
            role=row.RoleCode,
            profileTypeId=row.ProfileTypeId,
            profileTypeName=row.ProfileTypeName,
            email=row.Email,
            fullName=row.FullName,
            accountStatus=row.AccountStatus,
            memberSince=row.CreatedDatetime.strftime("%d %b %Y"),
        ), 200
    finally:
        conn.close()


@app.post("/api/auth/logout")
def logout():
    data = request.get_json(silent=True) or {}
    user_id = data.get("userId")

    if not user_id:
        return jsonify(message="Missing user"), 400

    conn = get_connection()
    try:
        cur = conn.cursor()
        cur.execute("SELECT Username FROM Users WHERE UserId = ?", user_id)
        user = cur.fetchone()
        if not user:
            return jsonify(message="Account not found"), 404

        # Close the most recent still-open session for this user, if any.
        cur.execute(
            "UPDATE TOP (1) Sessions SET LogoutDatetime = SYSUTCDATETIME(), SessionStatus = 'LOGGED_OUT' "
            "WHERE SessionId = ("
            "  SELECT TOP (1) SessionId FROM Sessions "
            "  WHERE UserId = ? AND SessionStatus = 'ACTIVE' ORDER BY LoginDatetime DESC"
            ")",
            user_id,
        )

        log_audit(cur, user_id, "LOGOUT", None, f"{user.Username} signed out")
        conn.commit()
        return jsonify(message="Logged out"), 200
    finally:
        conn.close()


@app.post("/api/auth/reset-password")
def self_reset_password():
    """Self-service "forgot password" — no login required, identified by
    email + username matching an existing account."""
    data = request.get_json(silent=True) or {}
    email = (data.get("email") or "").strip()
    username = (data.get("username") or "").strip()
    new_password = data.get("newPassword") or ""

    if not all([email, username, new_password]):
        return jsonify(message="Please fill in every field"), 400

    if len(new_password) < 6:
        return jsonify(message="Password must be at least 6 characters"), 400

    conn = get_connection()
    try:
        cur = conn.cursor()
        cur.execute("SELECT UserId FROM Users WHERE Username = ? AND Email = ?", username, email)
        user = cur.fetchone()
        if not user:
            return jsonify(message="No account matches that email and username"), 404

        cur.execute("UPDATE Users SET PasswordHash = ? WHERE UserId = ?",
                    generate_password_hash(new_password), user.UserId)
        log_audit(cur, user.UserId, "PASSWORD_RESET", None, f"{username} reset their own password")
        conn.commit()
        return jsonify(message="Password reset. Please sign in."), 200
    finally:
        conn.close()


@app.get("/api/audit-log")
def audit_log():
    conn = get_connection()
    try:
        cur = conn.cursor()
        cur.execute(
            "SELECT TOP 50 a.AuditId, a.ActionPerformed, a.AffectedRecord, a.ActionDatetime, a.Details, "
            "       u.Username, p.ProfileTypeName, p.RoleCode "
            "FROM AuditLog a "
            "JOIN Users u ON u.UserId = a.UserId "
            "JOIN ProfileTypes p ON p.ProfileTypeId = u.ProfileTypeId "
            "ORDER BY a.ActionDatetime DESC"
        )
        entries = [
            {
                "id": row.AuditId,
                "username": row.Username,
                "role": row.RoleCode,
                "profileTypeName": row.ProfileTypeName,
                "action": row.ActionPerformed,
                "affectedRecord": row.AffectedRecord,
                "details": row.Details,
                "timestamp": row.ActionDatetime.strftime("%d %b %Y, %H:%M:%S"),
            }
            for row in cur.fetchall()
        ]
        return jsonify(entries=entries), 200
    finally:
        conn.close()


# --------------------------------------------------------- profile types --

@app.get("/api/profile-types")
def list_profile_types():
    conn = get_connection()
    try:
        cur = conn.cursor()
        cur.execute("SELECT ProfileTypeId, RoleCode, ProfileTypeName, Description, Permissions "
                     "FROM ProfileTypes ORDER BY ProfileTypeId")
        return jsonify(profileTypes=[row_to_profile_type(r) for r in cur.fetchall()]), 200
    finally:
        conn.close()


@app.put("/api/profile-types/<int:profile_type_id>")
def update_profile_type(profile_type_id):
    """Renames a profile type / edits its description & permissions text.
    The RoleCode and ID are fixed — the app's dashboards route on RoleCode,
    not on this name."""
    data = request.get_json(silent=True) or {}
    name = (data.get("name") or "").strip()
    description = (data.get("description") or "").strip()
    permissions = (data.get("permissions") or "").strip()
    updated_by_user_id = data.get("updatedByUserId")

    if not name:
        return jsonify(message="Profile type name is required"), 400

    conn = get_connection()
    try:
        cur = conn.cursor()
        cur.execute("SELECT ProfileTypeName FROM ProfileTypes WHERE ProfileTypeId = ?", profile_type_id)
        existing = cur.fetchone()
        if not existing:
            return jsonify(message="Profile type not found"), 404

        cur.execute(
            "UPDATE ProfileTypes SET ProfileTypeName = ?, Description = ?, Permissions = ? WHERE ProfileTypeId = ?",
            name, description or None, permissions or None, profile_type_id,
        )
        if updated_by_user_id:
            log_audit(cur, updated_by_user_id, "PROFILE_TYPE_UPDATED", name,
                       f"Updated the {existing.ProfileTypeName} profile type")
        conn.commit()
        return jsonify(message="Profile type updated"), 200
    finally:
        conn.close()


# ------------------------------------------------------------ admin: users --

@app.get("/api/admin/users")
def list_users():
    conn = get_connection()
    try:
        cur = conn.cursor()
        cur.execute(USER_JOIN_QUERY + "ORDER BY u.UserId")
        return jsonify(users=[row_to_user(r) for r in cur.fetchall()]), 200
    finally:
        conn.close()


@app.get("/api/admin/users/<int:user_id>")
def get_user(user_id):
    conn = get_connection()
    try:
        cur = conn.cursor()
        cur.execute(USER_JOIN_QUERY + "WHERE u.UserId = ?", user_id)
        row = cur.fetchone()
        if not row:
            return jsonify(message="Account not found"), 404
        return jsonify(row_to_user(row)), 200
    finally:
        conn.close()


@app.post("/api/admin/create-user")
def admin_create_user():
    """Creates an account with an explicit profile type. Used by the User
    Admin's "Create User Account" screen — not reachable from public
    registration.

    Note: like the rest of this prototype, there's no session/token auth
    yet, so this isn't actually access-controlled server-side; it's kept
    separate from /api/auth/register so the app's own UI only ever offers
    it from inside the User Admin dashboard.
    """
    data = request.get_json(silent=True) or {}
    email = (data.get("email") or "").strip()
    full_name = (data.get("fullName") or "").strip()
    username = (data.get("username") or "").strip()
    password = data.get("password") or ""
    profile_type_id = data.get("profileTypeId")
    created_by_user_id = data.get("createdByUserId")

    if not all([email, full_name, username, password]) or not profile_type_id:
        return jsonify(message="Please fill in every field"), 400

    conn = get_connection()
    try:
        cur = conn.cursor()
        cur.execute("SELECT RoleCode, ProfileTypeName FROM ProfileTypes WHERE ProfileTypeId = ?", profile_type_id)
        profile_type = cur.fetchone()
        if not profile_type:
            return jsonify(message="Not a valid account type"), 400

        cur.execute("SELECT 1 FROM Users WHERE Username = ?", username)
        if cur.fetchone():
            return jsonify(message="That username is already taken"), 409

        password_hash = generate_password_hash(password)
        cur.execute(
            "INSERT INTO Users (ProfileTypeId, Username, FullName, Email, PasswordHash, AccountStatus) "
            "VALUES (?, ?, ?, ?, ?, 'ACTIVE')",
            profile_type_id, username, full_name, email, password_hash,
        )
        if created_by_user_id:
            log_audit(cur, created_by_user_id, "ACCOUNT_CREATED", username,
                       f"Created {username} as {profile_type.ProfileTypeName}")
        conn.commit()
        return jsonify(message="Account created"), 201
    finally:
        conn.close()


@app.put("/api/admin/users/<int:user_id>")
def update_user(user_id):
    data = request.get_json(silent=True) or {}
    full_name = (data.get("fullName") or "").strip()
    email = (data.get("email") or "").strip()
    profile_type_id = data.get("profileTypeId")
    account_status = (data.get("accountStatus") or "").strip().upper()
    updated_by_user_id = data.get("updatedByUserId")

    if not all([full_name, email]) or not profile_type_id or account_status not in ("ACTIVE", "INACTIVE"):
        return jsonify(message="Please fill in every field"), 400

    conn = get_connection()
    try:
        cur = conn.cursor()
        cur.execute("SELECT Username FROM Users WHERE UserId = ?", user_id)
        user = cur.fetchone()
        if not user:
            return jsonify(message="Account not found"), 404

        cur.execute("SELECT RoleCode FROM ProfileTypes WHERE ProfileTypeId = ?", profile_type_id)
        if not cur.fetchone():
            return jsonify(message="Not a valid account type"), 400

        cur.execute(
            "UPDATE Users SET FullName = ?, Email = ?, ProfileTypeId = ?, AccountStatus = ? WHERE UserId = ?",
            full_name, email, profile_type_id, account_status, user_id,
        )
        if updated_by_user_id:
            log_audit(cur, updated_by_user_id, "ACCOUNT_UPDATED", user.Username,
                       f"Updated {user.Username}'s account details")
        conn.commit()
        return jsonify(message="Account updated"), 200
    finally:
        conn.close()


@app.post("/api/admin/users/<int:user_id>/reset-password")
def admin_reset_password(user_id):
    data = request.get_json(silent=True) or {}
    new_password = data.get("newPassword") or ""
    reset_by_user_id = data.get("resetByUserId")

    if len(new_password) < 6:
        return jsonify(message="Password must be at least 6 characters"), 400

    conn = get_connection()
    try:
        cur = conn.cursor()
        cur.execute("SELECT Username FROM Users WHERE UserId = ?", user_id)
        user = cur.fetchone()
        if not user:
            return jsonify(message="Account not found"), 404

        cur.execute("UPDATE Users SET PasswordHash = ? WHERE UserId = ?",
                    generate_password_hash(new_password), user_id)
        if reset_by_user_id:
            log_audit(cur, reset_by_user_id, "PASSWORD_RESET", user.Username,
                       f"Reset {user.Username}'s password")
        conn.commit()
        return jsonify(message="Password reset"), 200
    finally:
        conn.close()


@app.get("/api/admin/session-stats")
def session_stats():
    """Distinct logged-in users per day, for the trailing 7 days ending today (UTC)."""
    today = datetime.now(timezone.utc).date()
    start = today - timedelta(days=6)

    conn = get_connection()
    try:
        cur = conn.cursor()
        cur.execute(
            "SELECT CAST(LoginDatetime AS DATE) AS Day, COUNT(DISTINCT UserId) AS Cnt "
            "FROM Sessions WHERE LoginDatetime >= ? "
            "GROUP BY CAST(LoginDatetime AS DATE)",
            start,
        )
        counts = {row.Day: row.Cnt for row in cur.fetchall()}

        days = []
        for i in range(7):
            day = start + timedelta(days=i)
            days.append({
                "date": day.strftime("%Y-%m-%d"),
                "label": day.strftime("%a"),
                "count": counts.get(day, 0),
            })
        return jsonify(days=days), 200
    finally:
        conn.close()


# ------------------------------------------------------------- self-service --

@app.put("/api/users/<int:user_id>")
def update_my_profile(user_id):
    """Self-service profile edit — full name and email only (no profile
    type or status; that's admin-only via /api/admin/users/<id>)."""
    data = request.get_json(silent=True) or {}
    full_name = (data.get("fullName") or "").strip()
    email = (data.get("email") or "").strip()

    if not all([full_name, email]):
        return jsonify(message="Please fill in every field"), 400

    conn = get_connection()
    try:
        cur = conn.cursor()
        cur.execute("SELECT Username FROM Users WHERE UserId = ?", user_id)
        user = cur.fetchone()
        if not user:
            return jsonify(message="Account not found"), 404

        cur.execute("UPDATE Users SET FullName = ?, Email = ? WHERE UserId = ?", full_name, email, user_id)
        log_audit(cur, user_id, "ACCOUNT_UPDATED", None, f"{user.Username} updated their own account details")
        conn.commit()
        return jsonify(message="Account updated", fullName=full_name, email=email), 200
    finally:
        conn.close()


# ------------------------------------------------------------- collectibles --
# Static stub for VS-09/10/11 (view inventory / filter / search) — there's no
# Collectibles table yet. This list is the shared master catalog every
# visitor's app reads; "discovered" status itself is tracked locally on each
# device for now (see DiscoveryStore.java) since there's no per-visitor
# persistence here either. Once System Admin's collectible management moves
# to the separate website and gets a real table, swap this list for a query
# and the endpoint's response shape stays the same, so the Android side
# doesn't need to change.
COLLECTIBLES = [
    {"id": "0042", "name": "Bandstand", "category": "Architecture", "rarity": "Common",
     "scientificName": None,
     "description": "A Victorian-era gazebo at the heart of the gardens, a favourite spot for weekend performances.",
     "feature": "Cast-iron columns, octagonal roof", "funFact": "Still hosts live performances today"},
    {"id": "0055", "name": "Vanda Miss Joaquim", "category": "Flora", "rarity": "Rare",
     "scientificName": "Papilionanthe Miss Joaquim",
     "description": "Singapore's national flower, a natural hybrid orchid first bred in a garden in 1893.",
     "feature": "Magenta petals, slender sepals", "funFact": "Singapore's national flower since 1981"},
    {"id": "0061", "name": "Vanda Tan Chay Yan", "category": "Flora", "rarity": "Common",
     "scientificName": "Vanda Tan Chay Yan",
     "description": "A hardy orchid hybrid named after one of Malaya's pioneering rubber planters.",
     "feature": "Deep purple blooms, waxy leaves", "funFact": "Named after Malaya's first rubber planter"},
    {"id": "0087", "name": "Tembusu Tree", "category": "Flora", "rarity": "Rare",
     "scientificName": "Cyrtophyllum fragrans",
     "description": "A heritage tree native to Southeast Asia, recognisable by its dark fissured bark.",
     "feature": "Dark fissured bark, cream flowers", "funFact": "Appears on the back of the Singapore $5 note"},
    {"id": "0093", "name": "Swan Lake", "category": "Architecture", "rarity": "Common",
     "scientificName": None,
     "description": "The gardens' oldest ornamental lake, home to a pair of black swans since the 1930s.",
     "feature": "Ornamental lake, resident black swans", "funFact": "The gardens' oldest water feature"},
    {"id": "0104", "name": "Rain Tree", "category": "Flora", "rarity": "Common",
     "scientificName": "Samanea saman",
     "description": "A broad-canopied shade tree whose leaflets fold up at dusk or before rain.",
     "feature": "Umbrella canopy, pink puff flowers", "funFact": "Leaflets fold up before rain"},
    {"id": "0118", "name": "Sundial Garden", "category": "Architecture", "rarity": "Rare",
     "scientificName": None,
     "description": "A heritage sundial marking the gardens' original 19th-century layout.",
     "feature": "Bronze gnomon, stone dial plate", "funFact": "Marks the gardens' original 19th-century layout"},
    {"id": "0131", "name": "Rafflesia", "category": "Flora", "rarity": "Rare",
     "scientificName": "Rafflesia arnoldii",
     "description": "One of the world's largest flowers, prized (and rarely seen in bloom) for its size and scent.",
     "feature": "Huge mottled petals, strong odour", "funFact": "One of the world's largest single flowers"},
]


@app.get("/api/collectibles")
def list_collectibles():
    return jsonify(collectibles=COLLECTIBLES), 200

@app.post("/api/identify")
def identify_plant():
    photo = request.files.get("image")
    if photo is None or not photo.filename:
        return jsonify(error="No image uploaded."), 400
    if photo.mimetype not in ("image/jpeg", "image/png"):
        return jsonify(error="Upload a JPEG or PNG image."), 415

    api_key = os.getenv("PLANTNET_API_KEY")
    if not api_key:
        return jsonify(error="Pl@ntNet API key is missing."), 500

    try:
        response = requests.post(
            "https://my-api.plantnet.org/v2/identify/all",
            params={
                "api-key": api_key,
                "include-related-images": "true",
                "nb-results": 1,
            },
            files={"images": (photo.filename, photo.stream, photo.mimetype)},
            timeout=30,
        )
    except requests.RequestException:
        return jsonify(error="Could not reach Pl@ntNet."), 502

    if response.status_code == 404:
        return jsonify(matched=False, results=[]), 200
    if not response.ok:
        return jsonify(error="Pl@ntNet returned an error.",
                       status=response.status_code), 502

    try:
        return jsonify(response.json()), 200
    except ValueError:
        return jsonify(error="Invalid response from Pl@ntNet."), 502

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000, debug=True)
