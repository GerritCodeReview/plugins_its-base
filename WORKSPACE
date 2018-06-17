workspace(name = "its_base")

load("//:bazlets.bzl", "load_bazlets")

load_bazlets(
    commit = "7ce9a3f58f030635b3a1229b7fb4c62ee7979bc8",
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
# gerrit_api_maven_local()
