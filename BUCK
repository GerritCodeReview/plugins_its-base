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

include_defs('//bucklets/gerrit_plugin.bucklet')

DEPS = [
  '//lib/commons:lang',
  '//lib:guava',
  '//lib/guice:guice',
  '//lib/jgit:jgit',
  '//lib/log:api',
  '//lib:velocity',
]

gerrit_plugin(
  name = 'its-base',
  srcs = glob(['src/main/java/**/*.java']),
  resources = glob(['src/main/resources/**/*']),
  provided_deps = DEPS,
)

TEST_UTIL_SRC = glob(['src/test/java/com/googlesource/gerrit/plugins/its/base/testutil/**/*.java'])

java_library(
  name = 'its-base_tests-utils',
  srcs = TEST_UTIL_SRC,
  deps = DEPS + [
    '//lib/easymock:easymock',
    '//lib/log:impl_log4j',
    '//lib/log:log4j',
    '//lib:junit',
    '//lib/powermock:powermock-api-easymock',
    '//lib/powermock:powermock-api-support',
    '//lib/powermock:powermock-core',
    '//lib/powermock:powermock-module-junit4',
    '//lib/powermock:powermock-module-junit4-common',
  ],
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
  deps = DEPS + [
    ':its-base__plugin',
    ':its-base_tests-utils',
    '//gerrit-plugin-api:lib',
    '//lib:gwtorm',
    '//lib/easymock:easymock',
    '//lib/log:impl_log4j',
    '//lib/log:log4j',
    '//lib:junit',
    '//lib/powermock:powermock-api-easymock',
    '//lib/powermock:powermock-api-support',
    '//lib/powermock:powermock-core',
    '//lib/powermock:powermock-module-junit4',
    '//lib/powermock:powermock-module-junit4-common',
  ],
)
