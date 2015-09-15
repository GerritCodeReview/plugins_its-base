include_defs('//bucklets/gerrit_plugin.bucklet')
include_defs('//bucklets/java_sources.bucklet')

SOURCES = glob(['src/main/java/**/*.java'])
RESOURCES = glob(['src/main/resources/**/*'])

DEPS = [
  '//lib/commons:lang',
  '//lib:guava',
  '//lib/guice:guice',
  '//lib/jgit:jgit',
  '//lib/log:api',
  '//lib:velocity',
]

TEST_DEPS = GERRIT_PLUGIN_API + [
  '//lib/easymock:easymock',
  '//lib/log:impl_log4j',
  '//lib/log:log4j',
  '//lib:gwtorm',
  '//lib:junit',
  '//lib/powermock:powermock-api-easymock',
  '//lib/powermock:powermock-api-support',
  '//lib/powermock:powermock-core',
  '//lib/powermock:powermock-module-junit4',
  '//lib/powermock:powermock-module-junit4-common',
]

gerrit_plugin(
  name = 'its-base',
  srcs = SOURCES,
  resources = RESOURCES,
  provided_deps = DEPS,
)

TEST_UTIL_SRC = glob(['src/test/java/com/googlesource/gerrit/plugins/its/base/testutil/**/*.java'])

java_library(
  name = 'its-base_tests-utils',
  srcs = TEST_UTIL_SRC,
  deps = DEPS + TEST_DEPS,
  visibility = ['PUBLIC'],
)

java_library(
  name = 'classpath',
  deps = DEPS + TEST_DEPS
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
  deps = DEPS + TEST_DEPS + [
    ':its-base__plugin',
    ':its-base_tests-utils',
  ],
)
