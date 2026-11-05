package com.example.plantdex.common.entity;

import org.json.JSONObject;

/**
 * A single entry in the shared collectible catalog (a species or a piece of
 * architecture visitors can discover). Sourced from GET /api/collectibles,
 * which reads the portal's Collectible table. Called with ?userId=, the server
 * also says whether this visitor has discovered it (its CollectibleUnlock rows);
 * DiscoveryStore keeps a copy of that on the phone.
 */
public class Collectible {

    private final String id;
    private final String name;
    private final String category; // "Flora" or "Architecture"
    private final String rarity; // "Common", "Uncommon", "Rare" or "Hidden"
    private final String scientificName; // null for Architecture entries
    private final String description;
    private final String feature; // short distinguishing feature, shown on the identification result
    private final String funFact;
    private final String imageUrl; // card picture, "" if none
    private final boolean discovered; // by the logged-in visitor, according to the server
    private final String unlockedAt; // ISO date-time from the server, null if not discovered

    public Collectible(String id, String name, String category, String rarity, String scientificName,
                        String description, String feature, String funFact) {
        this(id, name, category, rarity, scientificName, description, feature, funFact, "", false, null);
    }

    public Collectible(String id, String name, String category, String rarity, String scientificName,
                        String description, String feature, String funFact,
                        String imageUrl, boolean discovered, String unlockedAt) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.rarity = rarity;
        this.scientificName = scientificName;
        this.description = description;
        this.feature = feature;
        this.funFact = funFact;
        this.imageUrl = imageUrl;
        this.discovered = discovered;
        this.unlockedAt = unlockedAt;
    }

    public static Collectible fromJson(JSONObject json) {
        return new Collectible(
                json.optString("id", ""),
                json.optString("name", ""),
                json.optString("category", ""),
                json.optString("rarity", ""),
                json.isNull("scientificName") ? null : json.optString("scientificName", null),
                json.optString("description", ""),
                json.optString("feature", ""),
                json.optString("funFact", ""),
                json.optString("imageUrl", ""),
                json.optBoolean("discovered", false),
                json.isNull("unlockedAt") ? null : json.optString("unlockedAt", null));
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public boolean isDiscoveredOnServer() {
        return discovered;
    }

    public String getUnlockedAt() {
        return unlockedAt;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public String getRarity() {
        return rarity;
    }

    public String getScientificName() {
        return scientificName;
    }

    public String getDescription() {
        return description;
    }

    public String getFeature() {
        return feature;
    }

    public String getFunFact() {
        return funFact;
    }
}
