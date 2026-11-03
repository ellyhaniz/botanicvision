"""
Store uploaded files INSIDE THE DATABASE instead of in a local media/ folder.

Why: with a shared Azure SQL database, a picture saved in one person's media/ folder
does not exist for anyone else. Rows in the `StoredFile` table are shared by everyone
who uses the same database.

A model field uses it like this:
    image = models.ImageField(upload_to="collectibles/%Y/%m/", storage=database_storage)
The rest of Django (forms, field.url, field.delete()) works unchanged.
"""
import mimetypes

from django.core.files.base import ContentFile
from django.core.files.storage import Storage
from django.urls import reverse
from django.utils.deconstruct import deconstructible


@deconstructible
class DatabaseStorage(Storage):
    """A Django storage backend that keeps file contents in the portal_storedfile table."""

    # Imported inside the methods: this module is loaded while models are still loading.
    @staticmethod
    def _model():
        from .models import StoredFile
        return StoredFile

    def _open(self, name, mode="rb"):
        try:
            stored = self._model().objects.get(name=name)
        except self._model().DoesNotExist:
            raise FileNotFoundError(name)
        return ContentFile(bytes(stored.content), name=name)

    def _save(self, name, content):
        data = b"".join(content.chunks())
        content_type = mimetypes.guess_type(name)[0] or "application/octet-stream"
        self._model().objects.update_or_create(
            name=name,
            defaults={"content": data, "content_type": content_type, "size": len(data)},
        )
        return name

    def exists(self, name):
        return self._model().objects.filter(name=name).exists()

    def delete(self, name):
        if name:
            self._model().objects.filter(name=name).delete()

    def size(self, name):
        return self._model().objects.only("size").get(name=name).size

    def url(self, name):
        return reverse("stored_file", kwargs={"name": name})


def database_storage():
    """Callable used in model fields (Django migrations store this reference)."""
    return DatabaseStorage()
