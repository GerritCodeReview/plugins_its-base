
DEPS = [
  '//lib/commons:lang',
  '//lib:guava',
  '//lib/guice:guice',
  '//lib/jgit:jgit',
  '//lib/log:api',
  '//lib:parboiled-core',
  '//lib:velocity',
]

gerrit_plugin(
  name = 'its-base',
  srcs = glob(['src/main/java/**/*.java']),
  resources = glob(['src/main/resources/**/*']),
  deps = DEPS,
)

TEST_UTIL_SRC = glob(['src/test/java/com/googlesource/gerrit/plugins/hooks/testutil/**/*.java'])

java_library(
  name = 'its-base_tests-utils',
  srcs = TEST_UTIL_SRC,
  deps = DEPS + [
    '//plugins/its-base/lib:easymock',
    '//lib/log:impl_log4j',
    '//lib/log:log4j',
    '//lib:junit',
    '//plugins/its-base/lib:powermock-api-easymock',
    '//plugins/its-base/lib:powermock-api-support',
    '//plugins/its-base/lib:powermock-core',
    '//plugins/its-base/lib:powermock-module-junit4',
    '//plugins/its-base/lib:powermock-module-junit4-common',
  ],
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
    '//plugins/its-base/lib:easymock',
    '//lib/log:impl_log4j',
    '//lib/log:log4j',
    '//lib:junit',
    '//plugins/its-base/lib:powermock-api-easymock',
    '//plugins/its-base/lib:powermock-api-support',
    '//plugins/its-base/lib:powermock-core',
    '//plugins/its-base/lib:powermock-module-junit4',
    '//plugins/its-base/lib:powermock-module-junit4-common',
  ],
)
