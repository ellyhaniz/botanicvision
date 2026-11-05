# PlantDex

**Every discovery becomes a collectible.** PlantDex is a mobile app for visitors to the
Singapore Botanic Gardens. Point your phone at a plant or landmark, and PlantDex
identifies it and unlocks a collectible card with its details. Staff manage the content
through a web portal.

FYP-26-S3-15 · CSIT321

## Features

**Visitor app (Android)**
- **Identify** plants and landmarks with the camera, using our own trained image model
- **Collect** a card for each new discovery: name, scientific name, description, fun fact and photo
- **Inventory** of unlocked cards, plus locked "???" cards still to find, with search and filters
- **Progress** on the home screen and account page (e.g. 2 / 14 discovered)
- **Share** a discovery as a picture with the name, rarity and message on it
- **Notifications** for new discoveries and new plants or landmarks to find

**Staff portal (web)**
- **Researchers** manage botanical and architecture records
- **System Admins** manage collectible cards and link each one to the model's scan label
- **User Admins** manage staff accounts and roles
- Audit log of staff actions

## How it fits together

    Android app ──HTTP──▶ Django portal (/api/...) ──▶ Azure SQL database
                                │
                                └─▶ PlantDex image model (TensorFlow Lite)

- The app talks only to the portal's API. It never connects to the database directly.
- A photo is sent to `/api/identify`. If the model is confident (≥ 60%) and the plant or
  landmark has a card, that card is unlocked for the visitor.
- Collectibles, unlocks and accounts are stored in the shared Azure SQL database.

## Image model

A MobileNet-based classifier trained on our own photos from the Gardens plus iNaturalist
images. It recognises **13 classes**: 10 plants (e.g. Golden Trumpet, Peace Lily, Vanda
Orchid, Spider Lily) and 3 landmarks (Tanglin Gate, Swan Lake, Swing Me Mama).

The trained model is in `portal/ml/` (`plantdex_model.tflite` + `labels.txt`).

## Repository layout

| Folder | What's in it |
|---|---|
| `app/` | Android app (Java) |
| `portal/` | Django staff portal and the app's API (`portal/portal/api.py`) |
| `portal/ml/` | The trained image model used by `/api/identify` |
| `backend/` | Earlier Flask API, no longer used by the app (replaced by the portal) |

## Tech stack

Android (Java) · Python 3.13 · Django 6.1 · Azure SQL Database (mssql-django) ·
TensorFlow / LiteRT

## Getting started

See **[TEAM_GUIDE.md](TEAM_GUIDE.md)** for setting up, pulling the latest code, launching
the portal, and running the app on a phone. Portal-only details are in
[portal/README.md](portal/README.md) and [portal/INSTALLATION.md](portal/INSTALLATION.md).

> ⚠️ **Don't commit or push to `main`.** Work on your own branch and open a pull request.
