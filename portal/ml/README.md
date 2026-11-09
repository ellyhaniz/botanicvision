# PlantDex model files

`classifier.py` loads the two files in this folder:

- `plantdex_model.tflite`: the trained model
- `labels.txt`: its class names, one per line, in the model's output order

After retraining (`train.ipynb` in the FYP training folder), copy **both** new files
here and restart the portal. They must come from the same training run.
A class name that is new in `labels.txt` should also get a display name in
`CLASS_INFO` in `portal/classifier.py` (otherwise its folder name is shown).

The class `other` ("not in the collection") is special: a scan the model puts there
is treated as no match and unlocks nothing (`NOT_IN_COLLECTION` in `classifier.py`).
