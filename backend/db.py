import os

import pyodbc


def get_connection():
    """Opens a new connection to the SQL Server database.

    Configured entirely through environment variables (see docker-compose.yml)
    so the same code works whether SQL Server is in a container, on a
    separate Windows box, or in the cloud later.
    """
    server = os.environ.get("DB_SERVER", "localhost")
    database = os.environ.get("DB_NAME", "PlantDexDB")
    user = os.environ.get("DB_USER", "sa")
    password = os.environ.get("DB_PASSWORD")
    driver = os.environ.get("DB_DRIVER", "{ODBC Driver 18 for SQL Server}")

    if not password:
        raise RuntimeError("DB_PASSWORD environment variable is not set")

    conn_str = (
        f"DRIVER={driver};SERVER={server};DATABASE={database};"
        f"UID={user};PWD={password};TrustServerCertificate=yes;"
    )
    return pyodbc.connect(conn_str)
