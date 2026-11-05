package com.example.plantdex.visitor;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.plantdex.common.Session;
import com.example.plantdex.common.entity.Collectible;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Which collectibles the logged-in visitor has discovered, and when.
 * The real record is on the server (CollectibleUnlock rows, written when a scan
 * identifies the plant); this is the phone's copy of it, kept per visitor in
 * SharedPreferences and refreshed every time the catalog loads (syncFromServer),
 * so screens can check "discovered?" without waiting for the network.
 */
public final class DiscoveryStore {

    private static final String PREFS = "plantdex_discoveries";
    private static final String KEY_MAP = "discovered_map"; // + "_<userId>". JSON: {collectibleId: "dd MMM yyyy, HH:mm:ss"}
    private static final String TIMESTAMP_PATTERN = "dd MMM yyyy, HH:mm:ss";

    private static SimpleDateFormat timestampFormat() {
        return new SimpleDateFormat(TIMESTAMP_PATTERN, Locale.US);
    }

    private DiscoveryStore() {}

    /** Replaces this visitor's discoveries with what the server says (GET /api/collectibles?userId=). */
    public static void syncFromServer(Context context, List<Collectible> catalog) {
        JSONObject map = new JSONObject();
        for (Collectible c : catalog) {
            if (!c.isDiscoveredOnServer()) continue;
            try {
                map.put(c.getId(), displayTime(c.getUnlockedAt()));
            } catch (JSONException ignored) {
            }
        }
        save(context, map);
    }

    /** Marks a collectible discovered at the server's unlock time (ISO), e.g. right after a scan. */
    public static void markDiscovered(Context context, String collectibleId, String unlockedAtIso) {
        JSONObject map = map(context);
        try {
            map.put(collectibleId, displayTime(unlockedAtIso));
        } catch (JSONException e) {
            return;
        }
        save(context, map);
    }

    /** "2026-11-05T08:16:32.1+00:00" (server, UTC) -> "05 Nov 2026, 16:16:32" on this phone's clock. */
    private static String displayTime(String iso) {
        if (iso == null || iso.isEmpty()) return timestampFormat().format(new Date());
        try {
            Date date = Date.from(OffsetDateTime.parse(iso).toInstant());
            return timestampFormat().format(date);
        } catch (DateTimeParseException e) {
            return timestampFormat().format(new Date());
        }
    }

    public static boolean isDiscovered(Context context, String collectibleId) {
        return map(context).has(collectibleId);
    }

    /** Null if not discovered. */
    public static String getUnlockedAt(Context context, String collectibleId) {
        return map(context).optString(collectibleId, null);
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

    /** Each visitor gets their own map, so logging in as someone else on the same phone starts clean. */
    private static String key(Context context) {
        return KEY_MAP + "_" + Session.getUserId(context);
    }

    private static JSONObject map(Context context) {
        String raw = prefs(context).getString(key(context), null);
        if (raw == null) return new JSONObject();
        try {
            return new JSONObject(raw);
        } catch (JSONException e) {
            return new JSONObject();
        }
    }

    private static void save(Context context, JSONObject map) {
        prefs(context).edit().putString(key(context), map.toString()).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
