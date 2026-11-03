from django.contrib.auth.decorators import login_required
from django.http import Http404, HttpResponse, HttpResponseNotModified

from .models import StoredFile


@login_required
def stored_file(request, name):
    """Serve a file kept in the database (collectible pictures).

    Only logged-in portal users can fetch files. Only images are served inline; anything
    else is sent as a download, and browsers are told not to guess the type.
    """
    try:
        stored = StoredFile.objects.defer("content").get(name=name)
    except StoredFile.DoesNotExist:
        raise Http404("No such file")

    etag = f'"{stored.pk}-{stored.size}-{int(stored.created_at.timestamp())}"'
    if request.headers.get("If-None-Match") == etag:      # the browser already has it
        return HttpResponseNotModified()

    is_image = stored.content_type.startswith("image/")
    response = HttpResponse(
        bytes(stored.content),
        content_type=stored.content_type if is_image else "application/octet-stream",
    )
    response["ETag"] = etag
    response["Cache-Control"] = "private, max-age=3600"
    response["X-Content-Type-Options"] = "nosniff"
    if not is_image:
        response["Content-Disposition"] = "attachment"
    return response
