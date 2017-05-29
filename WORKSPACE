workspace(name = "its_base")

load("//:bazlets.bzl", "load_bazlets")

load_bazlets(
    commit = "e15ad03897f040435d6c5e808b697b1125b964c1",
    # local_path = "/home/<user>/projects/bazlets",
)

# Snapshot Plugin API
#load(
#    "@com_googlesource_gerrit_bazlets//:gerrit_api_maven_local.bzl",
#    "gerrit_api_maven_local",
#)

# Release Plugin API
load(
    "@com_googlesource_gerrit_bazlets//:gerrit_api.bzl",
    "gerrit_api",
)

# Load release Plugin API
gerrit_api()

# Load snapshot Plugin API
#gerrit_api_maven_local()

load(
    "@com_googlesource_gerrit_bazlets//tools:maven_jar.bzl",
    "maven_jar",
)

# Keep this version of Soy synchronized with the version used in gerrit.
maven_jar(
    name = "soy",
    artifact = "com.google.template:soy:2017-04-23",
    sha1 = "52f32a5a3801ab97e0909373ef7f73a3460d0802",
)
