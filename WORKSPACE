workspace(name = "its_base")

load("//:bazlets.bzl", "load_bazlets")

load_bazlets(
    commit = "8a4cbdc993f41fcfe7290e7d1007cfedf8d87c18",
#    local_path = "/home/davido/projects/bazlets",
)

# Snapshot Plugin API
load(
    "@com_googlesource_gerrit_bazlets//:gerrit_api_maven_local.bzl",
    "gerrit_api_maven_local",
)

# Release Plugin API
#load(
#    "@com_googlesource_gerrit_bazlets//:gerrit_api.bzl",
#    "gerrit_api",
#)
load(
    "@com_googlesource_gerrit_bazlets//:gerrit_gwt.bzl",
    "gerrit_gwt",
)

# Load release Plugin API
#gerrit_api()

# Load snapshot Plugin API
gerrit_api_maven_local()

gerrit_gwt()

load("@com_googlesource_gerrit_bazlets//tools:maven_jar.bzl", "maven_jar")

maven_jar(
    name = "commons_dbcp",
    artifact = "commons-dbcp:commons-dbcp:1.4",
    sha1 = "30be73c965cc990b153a100aaaaafcf239f82d39",
)
