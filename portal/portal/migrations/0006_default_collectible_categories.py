from django.db import migrations

DEFAULTS = [
    ("Flora", "Plants and flowers."),
    ("Architecture", "Heritage structures and buildings."),
]


def add_categories(apps, schema_editor):
    Category = apps.get_model("portal", "CollectibleCategory")
    for name, description in DEFAULTS:
        Category.objects.get_or_create(name=name, defaults={"description": description})


def remove_categories(apps, schema_editor):
    Category = apps.get_model("portal", "CollectibleCategory")
    # Only remove ones that nothing uses, so reversing never breaks collectibles.
    Category.objects.filter(name__in=[n for n, _ in DEFAULTS], collectibles__isnull=True).delete()


class Migration(migrations.Migration):

    dependencies = [
        ("portal", "0005_collectibles"),
    ]

    operations = [
        migrations.RunPython(add_categories, remove_categories),
    ]
