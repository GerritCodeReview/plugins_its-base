load("//tools/bzl:junit.bzl", "junit_tests")
load(
    "//tools/bzl:plugin.bzl",
    "gerrit_plugin",
    "PLUGIN_DEPS",
    "PLUGIN_TEST_DEPS",
)

gerrit_plugin(
  name = "its-base",
  srcs = glob(["src/main/java/**/*.java"]),
  resources = glob(["src/main/resources/**/*"]),
)

TEST_UTIL_SRC = glob(['src/test/java/com/googlesource/gerrit/plugins/its/base/testutil/**/*.java'])

java_library(
    name = 'its-base_tests-utils',
    srcs = TEST_UTIL_SRC,
    deps = PLUGIN_DEPS + PLUGIN_TEST_DEPS,
    testonly = 1,
    visibility = ['//visibility:public'],
)

junit_tests(
    name = "its_base_tests",
    srcs = glob(
      ["src/test/java/**/*.java"],
      exclude = TEST_UTIL_SRC
    ),
    tags = ["its-base"],
    deps = PLUGIN_DEPS + PLUGIN_TEST_DEPS + [
        ":its-base__plugin",
        ":its-base_tests-utils",
    ],
    testonly = 1,
)
