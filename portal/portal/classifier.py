"""
Runs the PlantDex image classifier (trained in the FYP notebook, train.ipynb) on
an uploaded photo.

The model files live in portal/ml/ (override with PLANTDEX_MODEL_DIR):
    plantdex_model.tflite   MobileNetV2, input 224x224 RGB 0-255, softmax output
    labels.txt              one class folder name per line, in output order
Always copy the two files together: the labels must come from the same training run.

The model is loaded once, on the first photo, and reused. If the files or the
LiteRT runtime are missing, available() is False and api.identify falls back to Pl@ntNet.
"""
import os
import threading
from pathlib import Path

from django.conf import settings
from PIL import Image, ImageOps

MODEL_DIR = Path(os.environ.get("PLANTDEX_MODEL_DIR", settings.BASE_DIR / "ml"))
MODEL_FILE = MODEL_DIR / "plantdex_model.tflite"
LABELS_FILE = MODEL_DIR / "labels.txt"
IMG_SIZE = 224
# The model's "none of these" class: photos of things outside the collection. A scan the
# model puts here is treated as no match, however sure it is.
NOT_IN_COLLECTION = "other"

# class folder name -> what the app shows. "scientific" is empty for landmarks,
# which hides the scientific-name row on the result screen.
CLASS_INFO = {
    "allamanda_cathartica":   {"scientific": "Allamanda cathartica", "common": "Golden Trumpet"},
    "calathea":               {"scientific": "Goeppertia", "common": "Calathea"},
    "cheilocostus_speciosus": {"scientific": "Cheilocostus speciosus", "common": "Crepe Ginger"},
    "costus":                 {"scientific": "Costus", "common": "Spiral Ginger"},
    "crinum_asiaticum":       {"scientific": "Crinum asiaticum", "common": "Giant Crinum Lily"},
    "dendrobium_hybrid":      {"scientific": "Dendrobium hybrid", "common": "Dendrobium Orchid"},
    "hymenocallis":           {"scientific": "Hymenocallis", "common": "Spider Lily"},
    "oncidium_hybrid":        {"scientific": "Oncidium hybrid", "common": "Dancing Lady Orchid"},
    "spathiphyllum":          {"scientific": "Spathiphyllum", "common": "Peace Lily"},
    "vanda_hybrid":           {"scientific": "Vanda hybrid", "common": "Vanda Orchid"},
    "swan_lake":              {"scientific": "", "common": "Swan Lake"},
    "swing_me_mama":          {"scientific": "", "common": "Swing Me Mama"},
    "tanglin_gate":           {"scientific": "", "common": "Tanglin Gate"},
}

_lock = threading.Lock()
_model = None   # (interpreter, labels) once loaded


def available():
    return MODEL_FILE.exists() and LABELS_FILE.exists()


def _load():
    global _model
    if _model is None:
        from ai_edge_litert.interpreter import Interpreter   # only needed once a model exists
        interpreter = Interpreter(model_path=str(MODEL_FILE))
        interpreter.allocate_tensors()
        labels = [l.strip() for l in LABELS_FILE.read_text(encoding="utf-8").splitlines() if l.strip()]
        n_out = interpreter.get_output_details()[0]["shape"][-1]
        if n_out != len(labels):
            raise RuntimeError(f"labels.txt has {len(labels)} classes but the model outputs {n_out}; "
                               "copy both files from the same training run.")
        _model = (interpreter, labels)
    return _model


def _prepare(photo):
    """Upright RGB 224x224 float32 batch, pixel values 0-255 (the model normalises them itself)."""
    import numpy as np
    image = ImageOps.exif_transpose(Image.open(photo)).convert("RGB")
    image = image.resize((IMG_SIZE, IMG_SIZE), Image.BILINEAR)
    return np.asarray(image, dtype=np.float32)[np.newaxis]


def classify(photo):
    """Returns [(label, probability), ...] for every class, most likely first."""
    batch = _prepare(photo)
    with _lock:   # one interpreter is shared, so run one photo at a time
        interpreter, labels = _load()
        interpreter.set_tensor(interpreter.get_input_details()[0]["index"], batch)
        interpreter.invoke()
        probs = interpreter.get_tensor(interpreter.get_output_details()[0]["index"])[0]
    return sorted(zip(labels, (float(p) for p in probs)), key=lambda x: x[1], reverse=True)


def as_plantnet_result(label, score):
    """One entry shaped like Pl@ntNet's results[], which is what the app already reads."""
    info = CLASS_INFO.get(label, {"scientific": "", "common": label.replace("_", " ").title()})
    return {
        "score": score,
        "label": label,
        "species": {
            "scientificNameWithoutAuthor": info["scientific"],
            "commonNames": [info["common"]],
        },
        "images": [],
    }
