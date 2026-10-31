package com.example.plantdex.common;

import com.example.plantdex.researcher.ResearcherDashboardActivity;
import com.example.plantdex.visitor.VisitorDashboardActivity;

/**
 * Shared role constants and the dashboard each one lands on. Visitor and
 * Researcher have their own dashboards; User Admin and System Admin work in
 * the Django portal, so any other role falls back to the Visitor dashboard
 * here rather than crashing.
 */
public final class Roles {

    public static final String VISITOR = "VISITOR";
    public static final String RESEARCHER = "RESEARCHER";

    private Roles() {}

    /** Which dashboard Activity a given role opens after login (or when "Home" is tapped in the bottom nav). */
    public static Class<?> dashboardFor(String role) {
        if (RESEARCHER.equals(role)) {
            return ResearcherDashboardActivity.class;
        }
        return VisitorDashboardActivity.class;
    }

    /** Whether this role's bottom nav includes the camera / identify shortcut. */
    public static boolean hasCameraTab(String role) {
        return true;
    }

    public static String displayId(String role) {
        return RESEARCHER.equals(role) ? "Researcher #ID" : "Visitor #ID";
    }
}
