package com.example.plantdex.common;

import com.example.plantdex.visitor.VisitorDashboardActivity;

/**
 * Shared role constant and the dashboard it lands on. PlantDex is now a
 * Visitor-only app — User Admin, System Admin and Researcher moved to a
 * separate management website (see "PlantDex Old" for that code as it was
 * before the split), so any account that isn't a Visitor still lands on
 * the Visitor dashboard here rather than crashing.
 */
public final class Roles {

    public static final String VISITOR = "VISITOR";

    private Roles() {}

    /** Which dashboard Activity a given role opens after login (or when "Home" is tapped in the bottom nav). */
    public static Class<?> dashboardFor(String role) {
        return VisitorDashboardActivity.class;
    }

    /** Whether this role's bottom nav includes the camera / identify shortcut. */
    public static boolean hasCameraTab(String role) {
        return true;
    }

    public static String displayId(String role) {
        return "Visitor #ID";
    }
}
