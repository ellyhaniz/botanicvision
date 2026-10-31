"""Creates the default User Admin test account (or resets its password if it
already exists). Run after schema.sql:

    docker compose exec api python seed_admin.py

Login afterwards with username "admin", password "Admin@123".
"""
from werkzeug.security import generate_password_hash

from db import get_connection

USERNAME = "admin"
PASSWORD = "Admin@123"
FULL_NAME = "Default User Admin"
EMAIL = "admin@plantdex.local"
USER_ADMIN_PROFILE_TYPE_ID = 1

conn = get_connection()
try:
    cur = conn.cursor()
    password_hash = generate_password_hash(PASSWORD)

    cur.execute("SELECT UserId FROM Users WHERE Username = ?", USERNAME)
    existing = cur.fetchone()

    if existing:
        cur.execute(
            "UPDATE Users SET PasswordHash = ?, ProfileTypeId = ?, AccountStatus = 'ACTIVE' WHERE UserId = ?",
            password_hash, USER_ADMIN_PROFILE_TYPE_ID, existing.UserId,
        )
        print(f"Updated existing '{USERNAME}' account (password reset to {PASSWORD!r}).")
    else:
        cur.execute(
            "INSERT INTO Users (ProfileTypeId, Username, FullName, Email, PasswordHash, AccountStatus) "
            "VALUES (?, ?, ?, ?, ?, 'ACTIVE')",
            USER_ADMIN_PROFILE_TYPE_ID, USERNAME, FULL_NAME, EMAIL, password_hash,
        )
        print(f"Created '{USERNAME}' account with password {PASSWORD!r}.")

    conn.commit()
finally:
    conn.close()
