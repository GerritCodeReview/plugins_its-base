workspace(name = "its_base")

load("//:bazlets.bzl", "load_bazlets")

load_bazlets(
    # commit = "e15ad03897f040435d6c5e808b697b1125b964c1",
    local_path = "/home/davido/projects/bazlets",
)

# Multiple version Plugin API
load(
    "@com_googlesource_gerrit_bazlets//:gerrit_api_multiversion.bzl",
    "gerrit_api_multiversion",
)

# Load release Plugin API
gerrit_api_multiversion()

# Load snapshot Plugin API
#gerrit_api_maven_local()
