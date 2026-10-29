package com.example.plantdex.common.entity;

import org.json.JSONObject;

/**
 * A single entry in the shared collectible catalog (a species or a piece of
 * architecture visitors can discover). Sourced from GET /api/collectibles —
 * currently a static list on the backend (see app.py), not yet its own DB
 * table. Whether a given visitor has *discovered* this item is tracked
 * separately and locally, in DiscoveryStore.
 */
public class Collectible {

    private final String id;
    private final String name;
    private final String category; // "Flora" or "Architecture"
    private final String rarity; // "Common" or "Rare"
    private final String scientificName; // null for Architecture entries
    private final String description;
    private final String feature; // short distinguishing feature, shown on the identification result
    private final String funFact;

    public Collectible(String id, String name, String category, String rarity, String scientificName,
                        String description, String feature, String funFact) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.rarity = rarity;
        this.scientificName = scientificName;
        this.description = description;
        this.feature = feature;
        this.funFact = funFact;
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
                json.optString("funFact", ""));
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
