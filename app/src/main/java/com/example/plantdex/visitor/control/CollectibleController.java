package com.example.plantdex.visitor.control;

import android.content.Context;

import com.example.plantdex.common.ApiClient;
import com.example.plantdex.common.Session;
import com.example.plantdex.common.entity.Collectible;
import com.example.plantdex.visitor.DiscoveryStore;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Business logic for reading the shared collectible catalog (#VS-09/10/11). */
public final class CollectibleController {

    private CollectibleController() {}

    public interface Callback {
        void onSuccess(List<Collectible> collectibles);
        void onError(String message);
    }

    /** Loads the catalog for the logged-in visitor and refreshes DiscoveryStore from it. */
    public static void loadAll(Context context, Callback callback) {
        Context app = context.getApplicationContext();
        ApiClient.getCollectibles(Session.getUserId(app), new ApiClient.Callback() {
            @Override
            public void onSuccess(JSONObject response) {
                JSONArray array = response.optJSONArray("collectibles");
                List<Collectible> collectibles = new ArrayList<>();
                if (array != null) {
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject item = array.optJSONObject(i);
                        if (item != null) collectibles.add(Collectible.fromJson(item));
                    }
                }
                DiscoveryStore.syncFromServer(app, collectibles);
                callback.onSuccess(collectibles);
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }
}
