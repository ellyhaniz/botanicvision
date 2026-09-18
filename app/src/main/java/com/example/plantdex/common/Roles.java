package com.example.plantdex.common;

import com.example.plantdex.visitor.VisitorDashboardActivity;

/** Shared role constants. Visitor is the only role with a dashboard so far;
 *  Researcher is in scope for its record-creation screens only. */
public final class Roles {

    public static final String VISITOR = "VISITOR";
    public static final String RESEARCHER = "RESEARCHER";

    private Roles() {}

    public static Class<?> dashboardFor(String role) {
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
