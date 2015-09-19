include_defs('//bucklets/gerrit_plugin.bucklet')

gerrit_plugin(
  name = 'its-base',
  srcs = glob(['src/main/java/**/*.java']),
  resources = glob(['src/main/resources/**/*']),
)

TEST_UTIL_SRC = glob(['src/test/java/com/googlesource/gerrit/plugins/its/base/testutil/**/*.java'])

java_library(
  name = 'its-base_tests-utils',
  srcs = TEST_UTIL_SRC,
  deps = GERRIT_PLUGIN_API,
  visibility = ['PUBLIC'],
)

java_test(
  name = 'its-base_tests',
  srcs = glob(
    ['src/test/java/**/*.java'],
    excludes = TEST_UTIL_SRC
  ),
  labels = ['its-base'],
  source_under_test = [':its-base__plugin'],
  deps = GERRIT_PLUGIN_API + [
    ':its-base__plugin',
    ':its-base_tests-utils',
  ],
)
