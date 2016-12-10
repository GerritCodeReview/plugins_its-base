# This plugin currently does not support a standalone build, as a
# standalone build was deemed too much maintenance overhead in its
# present (2015-09-20) form.
#
# Once the standalone build does no longer come with a maintenance
# overhead, a first shot at the standalone build for this plugin can
# be found at:
#
#   https://gerrit-review.googlesource.com/#/c/70896/
#

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

java_library(
    name = 'its-base_tests-utils',
    srcs = glob(["src/test/java/com/googlesource/gerrit/plugins/its/base/testutil/**/*.java"]),
    deps = PLUGIN_DEPS + PLUGIN_TEST_DEPS,
    visibility = ['//visibility:public'],
)

junit_tests(
    name = "its_base_tests",
    srcs = glob(
      ["src/test/java/**/*.java"],
      exclude = ["src/test/java/com/googlesource/gerrit/plugins/its/base/testutil/**/*.java"]
    ),
    tags = ["its-base"],
    deps = PLUGIN_DEPS + PLUGIN_TEST_DEPS + [
        ":its-base__plugin",
        ":its-base_tests-utils",
    ],
)
