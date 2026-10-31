//adding a storage so the collectible is working
package com.example.plantdex.visitor;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.plantdex.common.Session;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public final class ScannedCollectibleStore {
    private static final String PREFS = "plantdex_scanned_collectibles";

    private ScannedCollectibleStore() {}

    public static void save(Context context, String name, String scientificName,
                            double score, String scannedAt, String imageUrl,
                            String imageCredit) {
        int userId = Session.getUserId(context);
        if (userId <= 0) return;

        String common = clean(name);
        String scientific = clean(scientificName);
        String id = (scientific.isEmpty() ? common : scientific)
                .toLowerCase(Locale.ROOT);
        if (id.isEmpty() || id.equals("unknown plant")) return;

        JSONObject all = read(context, userId);
        JSONObject previous = all.optJSONObject(id);

        String firstSeen = previous == null
                ? clean(scannedAt)
                : previous.optString("scannedAt", clean(scannedAt));
        String photo = clean(imageUrl);
        String credit = clean(imageCredit);
        if (previous != null) {
            if (photo.isEmpty()) photo = previous.optString("imageUrl", "");
            if (credit.isEmpty()) credit = previous.optString("imageCredit", "");
        }

        try {
            JSONObject item = new JSONObject();
            item.put("name", common);
            item.put("scientificName", scientific);
            item.put("score", score);
            item.put("scannedAt", firstSeen);
            item.put("imageUrl", photo);
            item.put("imageCredit", credit);
            item.put("rarity", "Rare");

            all.put(id, item);
            prefs(context).edit()
                    .putString("user_" + userId, all.toString())
                    .apply();
        } catch (JSONException e) {
            Log.e("ScannedCollectibles", "Could not save collectible", e);
        }
    }

    public static List<JSONObject> getAll(Context context) {
        List<JSONObject> items = new ArrayList<>();
        int userId = Session.getUserId(context);
        if (userId <= 0) return items;

        JSONObject all = read(context, userId);
        Iterator<String> ids = all.keys();
        while (ids.hasNext()) {
            JSONObject item = all.optJSONObject(ids.next());
            if (item != null) items.add(item);
        }
        return items;
    }

    private static JSONObject read(Context context, int userId) {
        String saved = prefs(context).getString("user_" + userId, "{}");
        try {
            return new JSONObject(saved);
        } catch (JSONException e) {
            Log.e("ScannedCollectibles", "Could not read collectibles", e);
            return new JSONObject();
        }
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}