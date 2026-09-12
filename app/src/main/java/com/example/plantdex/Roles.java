package com.example.plantdex;

/** Shared role constants and the dashboard each one lands on. */
public final class Roles {

    public static final String VISITOR = "VISITOR";
    public static final String RESEARCHER = "RESEARCHER";
    public static final String USER_ADMIN = "USER ADMIN";
    public static final String SYSTEM_ADMIN = "SYSTEM ADMIN";

    private Roles() {}

    public static Class<?> dashboardFor(String role) {
        if (role == null) return VisitorDashboardActivity.class;
        switch (role) {
            case USER_ADMIN:
                return UserAdminDashboardActivity.class;
            case SYSTEM_ADMIN:
                return SystemAdminDashboardActivity.class;
            case RESEARCHER:
                return ResearcherDashboardActivity.class;
            default:
                return VisitorDashboardActivity.class;
        }
    }

    /** Whether this role's bottom nav includes the camera / identify shortcut. */
    public static boolean hasCameraTab(String role) {
        return VISITOR.equals(role);
    }

    public static String displayId(String role) {
        if (USER_ADMIN.equals(role)) return "User Admin #ID";
        if (SYSTEM_ADMIN.equals(role)) return "System Admin #ID";
        if (RESEARCHER.equals(role)) return "Researcher #ID";
        return "Visitor #ID";
    }
}
