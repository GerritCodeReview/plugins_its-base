include_defs('//bucklets/gerrit_plugin.bucklet')
include_defs('//bucklets/java_sources.bucklet')
include_defs('//bucklets/java_doc.bucklet')
include_defs('//bucklets/maven_package.bucklet')
include_defs(align_path('its-base', '//VERSION'))

SRCS = glob(['src/main/java/**/*.java'])

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
  srcs = SRCS,
  provided_deps = DEPS,
)

TEST_UTIL_SRC = glob(['src/test/java/com/googlesource/gerrit/plugins/hooks/testutil/**/*.java'])

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
)

maven_package(
  repository = 'gerrit-maven-repository',
  url = 'gs://gerrit-maven',
  version = PLUGIN_VERSION,
  group = 'com.googlesource.gerrit.plugins.its',
  jar = {'its-base': ':its-base__plugin'},
  src = {'its-base': ':its-base-src'},
  doc = {'its-base': ':its-base-javadoc'},
)

java_test(
  name = 'its-base_tests',
  srcs = glob(
    ['src/test/java/**/*.java'],
    excludes = TEST_UTIL_SRC
  ),
  labels = ['its-base'],
  source_under_test = [':its-base__plugin'],
  deps = GERRIT_PLUGIN_API + DEPS + [
    ':its-base__plugin',
    ':its-base_tests-utils',
    '//gerrit-extension-api:api',
    '//gerrit-reviewdb:client',
    '//gerrit-reviewdb:server',
    '//gerrit-server:server',
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

java_sources(
  name = 'its-base-src',
  srcs = SRCS,
)

java_doc(
  name = 'its-base-javadoc',
  title = 'ITS-Base API Documentation',
  pkg = 'com.googlesource.gerrit.plugins.hooks',
  paths = ['src/main/java'],
  srcs = glob([n + '**/*.java' for n in SRCS]),
  deps = GERRIT_PLUGIN_API,
)
