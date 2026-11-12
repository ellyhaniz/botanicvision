import portal.storage
from django.db import migrations, models


class Migration(migrations.Migration):
    """Record photos move from each computer's media/ folder into the shared StoredFile table.

    Only Django's record of the fields changes (which storage they use): the image column
    keeps holding the file name, so nothing is run against the database. Existing photos
    are copied in afterwards with `manage.py move_record_photos_to_db`.
    """

    dependencies = [
        ('portal', '0007_collectible_model_label'),
    ]

    operations = [
        migrations.SeparateDatabaseAndState(
            database_operations=[],
            state_operations=[
                migrations.AlterField(
                    model_name='architecturerecord',
                    name='image',
                    field=models.ImageField(storage=portal.storage.database_storage,
                                            upload_to='architecture/%Y/%m/'),
                ),
                migrations.AlterField(
                    model_name='botanicalrecord',
                    name='image',
                    field=models.ImageField(storage=portal.storage.database_storage,
                                            upload_to='botanical/%Y/%m/'),
                ),
            ],
        ),
    ]
