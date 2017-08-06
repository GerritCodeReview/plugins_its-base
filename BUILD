load("//tools/bzl:junit.bzl", "junit_tests")
load(
    "//tools/bzl:plugin.bzl",
    "gerrit_plugin",
    "PLUGIN_DEPS",
    "PLUGIN_DEPS_NEVERLINK",
    "PLUGIN_TEST_DEPS",
)

config_setting(
    name = "2_14_1",
    values = {
        "define": "api_2_14_1=1",
    },
)

config_setting(
    name = "2_14_2",
    values = {
        "define": "api_2_14_2=1",
    },
)

plugin_deps_neverlink_tmpl = "//external:gerrit-plugin-api-neverlink_%s"

plugin_deps_tmpl = "//external:gerrit-plugin-api_%s"

plugin_test_deps_tmpl = "//external:gerrit-acceptance-framework_%s"

plugin_deps_neverlink = select({
    ":2_14_1": [plugin_deps_neverlink_tmpl % "2.14.1"],
    ":2_14_2": [plugin_deps_neverlink_tmpl % "2.14.2"],
    "//conditions:default": PLUGIN_DEPS_NEVERLINK,
})

plugin_deps = select({
    ":2_14_1": [plugin_deps_tmpl % "2.14.1"],
    ":2_14_2": [plugin_deps_tmpl % "2.14.2"],
    "//conditions:default": PLUGIN_DEPS,
})

plugin_test_deps = select({
    ":2_14_1": [plugin_test_deps_tmpl % "2.14.1"],
    ":2_14_2": [plugin_test_deps_tmpl % "2.14.2"],
    "//conditions:default": PLUGIN_TEST_DEPS,
})

gerrit_plugin(
    name = "its-base",
    srcs = glob(["src/main/java/**/*.java"]),
    plugin_deps_neverlink = plugin_deps_neverlink,
    resources = glob(["src/main/resources/**/*"]),
)

TEST_UTIL_SRC = glob(["src/test/java/com/googlesource/gerrit/plugins/its/base/testutil/**/*.java"])

java_library(
    name = "its-base_tests-utils",
    testonly = 1,
    srcs = TEST_UTIL_SRC,
    visibility = ["//visibility:public"],
    deps = plugin_deps_neverlink + plugin_test_deps,
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
    exports = plugin_deps + plugin_test_deps + [
        ":its-base__plugin",
        ":its-base_tests-utils",
    ],
)
