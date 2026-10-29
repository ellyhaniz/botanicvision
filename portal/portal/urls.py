from django.contrib.auth import views as auth_views
from django.urls import path
from . import views, account_views, profile_views, error_log_views
from .records import architecture, botanical

urlpatterns = [
    path("", views.dashboard, name="dashboard"),
    path("login/", auth_views.LoginView.as_view(
        template_name="portal/login.html", redirect_authenticated_user=True), name="login"),
    path("logout/", auth_views.LogoutView.as_view(), name="logout"), 
    path("section/audit-logs/", views.audit_logs, name="audit_logs"),

    path("section/accounts/", account_views.account_list, name="accounts"),
    path("section/accounts/<int:pk>/", account_views.account_detail, name="account_detail"),
    path("section/accounts/<int:pk>/edit/", account_views.account_edit, name="account_edit"),
    path("section/accounts/<int:pk>/status/", account_views.account_status, name="account_status"),
    path("section/profile-types/", profile_views.profile_list, name="profile_types"),
    path("section/profile-types/<int:pk>/", profile_views.profile_detail, name="profile_detail"),
    path("section/profile-types/<int:pk>/edit/", profile_views.profile_edit, name="profile_edit"),
    path("section/error-logs/", error_log_views.error_log_list, name="error_logs"),
    path("section/error-logs/<int:pk>/", error_log_views.error_log_detail, name="error_log_detail"),

    path("password-reset/", auth_views.PasswordResetView.as_view(
        template_name="portal/password_reset_form.html",
        email_template_name="portal/password_reset_email.txt",
        subject_template_name="portal/password_reset_subject.txt"),
        name="password_reset"),
    path("password-reset/sent/", auth_views.PasswordResetDoneView.as_view(
        template_name="portal/password_reset_done.html"),
        name="password_reset_done"),
    path("reset/<uidb64>/<token>/", views.PortalPasswordResetConfirmView.as_view(),
        name="password_reset_confirm"),
    path("reset/done/", auth_views.PasswordResetCompleteView.as_view(
        template_name="portal/password_reset_complete.html"),
        name="password_reset_complete"),


    # Researcher records (botanical + architecture)
    *botanical.urls,
    *architecture.urls,

    # keep this LAST: placeholder page for sections that have no real page yet
    path("section/<slug:slug>/", views.section, name="section"),
]