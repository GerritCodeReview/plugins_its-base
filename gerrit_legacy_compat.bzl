load("//:version.bzl", "GERRIT_VERSION")

if GERRIT_VERSION > "2.15":
    SRCS_EXCLUDE = []
    SRCS_INCLUDE = [ "src/main/java/**/*.java" ]
else:
    SRCS_INCLUDE = [
        "src/main/java/**/*.java",
        "src/legacy/java/**/*.java",
    ]
    SRCS_EXCLUDE = [
        "src/main/java/**/ActionController.java",
        "src/main/java/**/ItsHookModule.java",
    ]
