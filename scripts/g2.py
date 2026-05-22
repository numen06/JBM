from pathlib import Path
TARGET = Path(r"d:\workspaces\JBM7\jbm-cluster\jbm-cluster-platform\jbm-cluster-platform-center\src\test\java\com\jbm\cluster\center\integration\CustomFormsOpenApiH2IT.java")
SRC = Path(r"d:\workspaces\JBM7\jbm-cluster\jbm-cluster-platform\jbm-cluster-platform-center\src\test\java\com\jbm\cluster\center\integration\ExtendFormDefinitionApiH2IT.java")
JAVA = SRC.read_text(encoding="utf-8")
# placeholder - will overwrite via second file
print(len(JAVA))