workspace(name = "its_base")

load("//:bazlets.bzl", "load_bazlets")

load_bazlets(
    commit = "42bffc66c0e92753133e4cea2debe65abc359c4d",
    # local_path = "/home/<user>/projects/bazlets",
)

# Snapshot Plugin API
load(
    "@com_googlesource_gerrit_bazlets//:gerrit_api_maven_local.bzl",
    "gerrit_api_maven_local",
)

# Release Plugin API
load(
    "@com_googlesource_gerrit_bazlets//:gerrit_api.bzl",
    "gerrit_api",
)

# Load release Plugin API
#gerrit_api()

# Load snapshot Plugin API
gerrit_api_maven_local()

load(
    "@com_googlesource_gerrit_bazlets//tools:maven_jar.bzl",
    "maven_jar",
)

maven_jar(
    name = "velocity",
    artifact = "org.apache.velocity:velocity:1.7",
    sha1 = "2ceb567b8f3f21118ecdec129fe1271dbc09aa7a",
)
