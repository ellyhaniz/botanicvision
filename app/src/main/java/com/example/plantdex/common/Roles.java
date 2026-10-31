package com.example.plantdex.common;

import com.example.plantdex.researcher.ResearcherDashboardActivity;
import com.example.plantdex.visitor.VisitorDashboardActivity;

/** Shared role constants. Visitor and Researcher each have their own
 *  dashboard; other roles fall back to the Visitor dashboard until they're
 *  built out. */
public final class Roles {

    public static final String VISITOR = "VISITOR";
    public static final String RESEARCHER = "RESEARCHER";

    private Roles() {}

    public static Class<?> dashboardFor(String role) {
        if (RESEARCHER.equals(role)) {
            return ResearcherDashboardActivity.class;
        }
        return VisitorDashboardActivity.class;
    }

    /** Whether the bottom nav includes the camera / identify shortcut. */
    public static boolean hasCameraTab(String role) {
        return true;
    }

    public static String displayId(String role) {
        return RESEARCHER.equals(role) ? "Researcher #ID" : "Visitor #ID";
    }
}
