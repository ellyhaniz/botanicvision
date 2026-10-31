package com.example.plantdex.visitor;

import com.example.plantdex.common.entity.Collectible;

import java.util.HashSet;
import java.util.Set;

/**
 * Combined filter state for the #VS-10 filter sheet — type, rarity and
 * discovery status. Immutable; VisitorSheets.showFilterInventory hands back
 * a new instance when "Apply" is tapped.
 */
public final class InventoryFilter {

    public static final String STATUS_ALL = "ALL";
    public static final String STATUS_COLLECTED = "COLLECTED";
    public static final String STATUS_UNDISCOVERED = "UNDISCOVERED";

    public final Set<String> types; // "Flora" / "Architecture" — empty means "all types"
    public final Set<String> rarities; // "Common" / "Rare" / "Legendary" — empty means "all rarities"
    public final String discoveryStatus; // one of STATUS_*

    public InventoryFilter() {
        this(new HashSet<>(), new HashSet<>(), STATUS_ALL);
    }

    public InventoryFilter(Set<String> types, Set<String> rarities, String discoveryStatus) {
        this.types = types;
        this.rarities = rarities;
        this.discoveryStatus = discoveryStatus;
    }

    public boolean isActive() {
        return !types.isEmpty() || !rarities.isEmpty() || !STATUS_ALL.equals(discoveryStatus);
    }

    public boolean matches(Collectible c, boolean discovered) {
        if (!types.isEmpty() && !types.contains(c.getCategory())) return false;
        if (!rarities.isEmpty() && !rarities.contains(c.getRarity())) return false;
        if (STATUS_COLLECTED.equals(discoveryStatus) && !discovered) return false;
        if (STATUS_UNDISCOVERED.equals(discoveryStatus) && discovered) return false;
        return true;
    }
}
