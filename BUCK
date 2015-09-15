include_defs('//bucklets/gerrit_plugin.bucklet')
include_defs('//bucklets/java_sources.bucklet')

SOURCES = glob(['src/main/java/**/*.java'])
RESOURCES = glob(['src/main/resources/**/*'])

gerrit_plugin(
  name = 'its-base',
  srcs = SOURCES,
  resources = RESOURCES,
)

TEST_UTIL_SRC = glob(['src/test/java/com/googlesource/gerrit/plugins/its/base/testutil/**/*.java'])

java_library(
  name = 'its-base_tests-utils',
  srcs = TEST_UTIL_SRC,
  deps = GERRIT_PLUGIN_API,
  visibility = ['PUBLIC'],
)

java_library(
  name = 'classpath',
  deps = GERRIT_PLUGIN_API,
)

java_sources(
  name = 'its-base-sources',
  srcs = SOURCES + RESOURCES,
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
