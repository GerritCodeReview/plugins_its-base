load("//:version.bzl", "GERRIT_VERSION")

if GERRIT_VERSION > "2.14":
    SRC_EXCLUDE = [
        "src/main/java/**/ItsHookModule.java",
        "src/main/java/**/ActionController.java",
    ]
else:
    SRC_EXCLUDE = [
        "src/main/java/com/googlesource/gerrit214/**/*.java",
    ]
