import shutil
from pathlib import Path
SCRIPTS = Path(__file__).resolve().parents[2]
shutil.copyfile(SCRIPTS / "run_center_rest_tests.py", SCRIPTS / "run_user_perm_rest_tests.py")
print("ok")