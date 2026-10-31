package com.example.plantdex.visitor;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

/**
 * Tracks which collectibles this device's visitor has discovered, and when.
 * Local-only for now (SharedPreferences) since there's no per-visitor
 * discovery table on the backend yet — see the note on GET /api/collectibles
 * in app.py. A real identification (#VS-06/07) doesn't feed into this yet
 * either; a handful of items are pre-seeded on first run so the inventory
 * isn't empty.
 */
public final class DiscoveryStore {

    private static final String PREFS = "plantdex_discoveries";
    private static final String KEY_MAP = "discovered_map"; // JSON: {collectibleId: "dd MMM yyyy, HH:mm:ss"}
    private static final String KEY_SEEDED = "seeded";
    private static final String TIMESTAMP_PATTERN = "dd MMM yyyy, HH:mm:ss";

    private static SimpleDateFormat timestampFormat() {
        return new SimpleDateFormat(TIMESTAMP_PATTERN, Locale.US);
    }

    private DiscoveryStore() {}

    public static boolean isDiscovered(Context context, String collectibleId) {
        return map(context).has(collectibleId);
    }

    /** Null if not discovered. */
    public static String getUnlockedAt(Context context, String collectibleId) {
        return map(context).optString(collectibleId, null);
    }

    /** Marks an item discovered right now, if it isn't already. */
    public static void markDiscovered(Context context, String collectibleId) {
        JSONObject map = map(context);
        if (map.has(collectibleId)) return;
        String now = timestampFormat().format(new Date());
        try {
            map.put(collectibleId, now);
        } catch (JSONException e) {
            return;
        }
        save(context, map);
    }

    public static Set<String> getDiscoveredIds(Context context) {
        JSONObject map = map(context);
        Set<String> ids = new HashSet<>();
        Iterator<String> keys = map.keys();
        while (keys.hasNext()) ids.add(keys.next());
        return ids;
    }

    /** The most recently discovered collectible's id, or null if nothing's been discovered yet. */
    public static String getMostRecentlyDiscoveredId(Context context) {
        JSONObject map = map(context);
        Iterator<String> keys = map.keys();
        String bestId = null;
        Date bestDate = null;
        while (keys.hasNext()) {
            String id = keys.next();
            try {
                Date date = timestampFormat().parse(map.optString(id, ""));
                if (date != null && (bestDate == null || date.after(bestDate))) {
                    bestDate = date;
                    bestId = id;
                }
            } catch (ParseException ignored) {
            }
        }
        return bestId;
    }

    /** Seeds a small starter set once per install, so the inventory has something in it on first run. */
    public static void seedDefaultsIfFirstRun(Context context) {
        SharedPreferences prefs = prefs(context);
        if (prefs.getBoolean(KEY_SEEDED, false)) return;

        JSONObject map = new JSONObject();
        try {
            map.put("0042", "05 Dec 2026, 14:13:09"); // Bandstand
            map.put("0055", "03 Dec 2026, 11:10:05"); // Vanda Miss Joaquim
            map.put("0087", "07 Dec 2026, 18:14:05"); // Tembusu Tree
        } catch (JSONException ignored) {
        }
        save(context, map);
        prefs.edit().putBoolean(KEY_SEEDED, true).apply();
    }

    private static JSONObject map(Context context) {
        String raw = prefs(context).getString(KEY_MAP, null);
        if (raw == null) return new JSONObject();
        try {
            return new JSONObject(raw);
        } catch (JSONException e) {
            return new JSONObject();
        }
    }

    private static void save(Context context, JSONObject map) {
        prefs(context).edit().putString(KEY_MAP, map.toString()).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
