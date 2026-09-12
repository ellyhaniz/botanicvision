package com.example.plantdex;

/** Shared role constants. Visitor is the only role currently in scope. */
public final class Roles {

    public static final String VISITOR = "VISITOR";

    private Roles() {}

    public static Class<?> dashboardFor(String role) {
        return VisitorDashboardActivity.class;
    }

    /** Whether the bottom nav includes the camera / identify shortcut. */
    public static boolean hasCameraTab(String role) {
        return true;
    }

    public static String displayId(String role) {
        return "Visitor #ID";
    }
}
