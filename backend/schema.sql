-- Run this in SSMS after connecting to the Dockerized SQL Server
-- (Server: localhost,1433 · Login: sa · Password: see backend/docker-compose.yml)
--
-- Drops and recreates Users, ProfileTypes, Sessions, and AuditLog (this is
-- dev/prototype data). Default admin account is seeded separately by
-- seed_admin.py, since its password needs real PBKDF2/scrypt hashing.

IF DB_ID('PlantDexDB') IS NULL
BEGIN
    CREATE DATABASE PlantDexDB;
END
GO

USE PlantDexDB;
GO

IF OBJECT_ID('dbo.AuditLog', 'U') IS NOT NULL DROP TABLE dbo.AuditLog;
IF OBJECT_ID('dbo.Sessions', 'U') IS NOT NULL DROP TABLE dbo.Sessions;
IF OBJECT_ID('dbo.Users', 'U') IS NOT NULL DROP TABLE dbo.Users;
IF OBJECT_ID('dbo.ProfileTypes', 'U') IS NOT NULL DROP TABLE dbo.ProfileTypes;
GO

-- The 4 account types are fixed by the app's own navigation code (each one
-- has its own dashboard), so RoleCode is NOT editable from the app — it's
-- the stable internal identifier. ProfileTypeName/Description/Permissions
-- ARE editable from the User Admin's Profile Type Management screen.
-- Permissions is descriptive text only — nothing in the app enforces it yet.
CREATE TABLE dbo.ProfileTypes (
    ProfileTypeId   INT           NOT NULL PRIMARY KEY,
    RoleCode        NVARCHAR(20)  NOT NULL UNIQUE,
    ProfileTypeName NVARCHAR(50)  NOT NULL,
    Description     NVARCHAR(500) NULL,
    Permissions     NVARCHAR(500) NULL
);
GO

INSERT INTO dbo.ProfileTypes (ProfileTypeId, RoleCode, ProfileTypeName, Description, Permissions) VALUES
    (1, 'USER ADMIN',   'User Admin',   'Manages user accounts and profile types.',
        'Manage user accounts, Manage profile types, View audit log'),
    (2, 'SYSTEM ADMIN', 'System Admin', 'Manages collectibles and system-level records.',
        'Manage collectibles, View error logs'),
    (3, 'RESEARCHER',   'Researcher',   'Verifies plant and architecture identification records.',
        'Verify identification records, Manage botanical/architecture records'),
    (4, 'VISITOR',      'Visitor',      'Discovers and collects plants and architecture around the gardens.',
        'Identify plants and buildings, View own collection');
GO

CREATE TABLE dbo.Users (
    UserId          INT IDENTITY(1,1) PRIMARY KEY,
    ProfileTypeId   INT           NOT NULL REFERENCES dbo.ProfileTypes(ProfileTypeId),
    Username        NVARCHAR(50)  NOT NULL UNIQUE,
    FullName        NVARCHAR(255) NOT NULL,
    Email           NVARCHAR(255) NOT NULL,
    PasswordHash    NVARCHAR(255) NOT NULL,
    AccountStatus   NVARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    CreatedDatetime DATETIME2     NOT NULL DEFAULT SYSUTCDATETIME()
);
GO

-- One row per login; logout fills in LogoutDatetime and flips the status.
-- Powers the User Admin dashboard's daily-active-users chart.
CREATE TABLE dbo.Sessions (
    SessionId      INT IDENTITY(1,1) PRIMARY KEY,
    UserId         INT       NOT NULL REFERENCES dbo.Users(UserId),
    LoginDatetime  DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
    LogoutDatetime DATETIME2 NULL,
    SessionStatus  NVARCHAR(20) NOT NULL DEFAULT 'ACTIVE'  -- ACTIVE or LOGGED_OUT
);
GO

-- UserId is who PERFORMED the action. AffectedRecord names what it was done
-- to, when that's a different account (e.g. a User Admin editing someone
-- else) — NULL when the action only affects the actor's own account.
CREATE TABLE dbo.AuditLog (
    AuditId         INT IDENTITY(1,1) PRIMARY KEY,
    UserId          INT           NOT NULL REFERENCES dbo.Users(UserId),
    ActionPerformed NVARCHAR(50)  NOT NULL,
    AffectedRecord  NVARCHAR(255) NULL,
    ActionDatetime  DATETIME2     NOT NULL DEFAULT SYSUTCDATETIME(),
    Details         NVARCHAR(500) NULL
);
GO
