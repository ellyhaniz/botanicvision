from django.contrib.auth.signals import (
    user_logged_in, user_logged_out, user_login_failed,
)
from django.dispatch import receiver

from .audit import log_action
from .models import AuditLog


@receiver(user_logged_in)
def on_login(sender, request, user, **kwargs):
    log_action(user, AuditLog.LOGIN, request=request)


@receiver(user_logged_out)
def on_logout(sender, request, user, **kwargs):
    if user is not None:               # can be None if the session had expired
        log_action(user, AuditLog.LOGOUT, request=request)


@receiver(user_login_failed)
def on_login_failed(sender, credentials, request=None, **kwargs):
    # Only record the username that was typed. Never store the password.
    log_action(None, AuditLog.LOGIN_FAILED,
               username=credentials.get("username", ""), request=request)