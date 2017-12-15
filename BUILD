load("//tools/bzl:junit.bzl", "junit_tests")
load(
    "//tools/bzl:plugin.bzl",
    "gerrit_plugin",
    "PLUGIN_DEPS",
    "PLUGIN_TEST_DEPS",
)

GERRIT_POST_214_EXCLUDE = [
    "src/main/java/com/googlesource/gerrit214/**/*.java",
]

GERRIT_214_EXCLUDE = [
    "src/main/java/**/ItsHookModule.java",
    "src/main/java/**/ActionController.java",
]

gerrit_plugin(
    name = "its-base",
    srcs = glob(
        ["src/main/java/**/*.java"],
        exclude = GERRIT_POST_214_EXCLUDE,
# When building on Gerrit 2.14, enable the 2.14-specific excludes
#        exclude = GERRIT_214_EXCLUDE,
    ),
    resources = glob(["src/main/resources/**/*"]),
)

TEST_UTIL_SRC = glob(["src/test/java/com/googlesource/gerrit/plugins/its/base/testutil/**/*.java"])

java_library(
    name = "its-base_tests-utils",
    testonly = 1,
    srcs = TEST_UTIL_SRC,
    visibility = ["//visibility:public"],
    deps = PLUGIN_DEPS + PLUGIN_TEST_DEPS,
)

junit_tests(
    name = "its_base_tests",
    testonly = 1,
    srcs = glob(
        ["src/test/java/**/*.java"],
        exclude = TEST_UTIL_SRC,
    ),
    tags = ["its-base"],
    deps = [
        "its-base__plugin_test_deps",
    ],
)

java_library(
    name = "its-base__plugin_test_deps",
    testonly = 1,
    visibility = ["//visibility:public"],
    exports = PLUGIN_DEPS + PLUGIN_TEST_DEPS + [
        ":its-base__plugin",
        ":its-base_tests-utils",
    ],
)
