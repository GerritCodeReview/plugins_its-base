load(
    "@com_googlesource_gerrit_bazlets//:gerrit_plugin.bzl",
    "gerrit_plugin",
    "gerrit_plugin_test_util",
    "gerrit_plugin_tests",
)

gerrit_plugin(
    plugin = "its-base",
    srcs = glob(["src/main/java/**/*.java"]),
    resources = glob(["src/main/resources/**/*"]),
)

TEST_UTIL_SRC = glob(["src/test/java/com/googlesource/gerrit/plugins/its/base/testutil/**/*.java"])

gerrit_plugin_test_util(
    name = "its-base_tests-utils",
    srcs = TEST_UTIL_SRC,
    visibility = ["//visibility:public"],
)

gerrit_plugin_tests(
    name = "its_base_tests",
    srcs = glob(
        ["src/test/java/**/*.java"],
        exclude = TEST_UTIL_SRC,
    ),
    plugin = "its-base",
    tags = ["its-base"],
    deps = [":its-base_tests-utils"],
)