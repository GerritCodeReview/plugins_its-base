include_defs('//bucklets/gerrit_plugin.bucklet')
include_defs('//bucklets/java_sources.bucklet')
include_defs('//bucklets/java_doc.bucklet')
include_defs('//bucklets/maven_package.bucklet')

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
  deps = GERRIT_PLUGIN_API + GERRIT_TESTS,
  visibility = ['PUBLIC'],
)

java_library(
  name = 'classpath',
  deps = GERRIT_PLUGIN_API + GERRIT_TESTS,
)

java_sources(
  name = 'its-base-sources',
  srcs = SOURCES + RESOURCES,
)

java_doc(
  name = 'its-base-javadoc',
  title = 'Its-base API Documentation',
  pkgs = ['com.googlesource.gerrit.plugins.its'],
  paths = ['src/main/java'],
  srcs = SOURCES,
  deps = GERRIT_PLUGIN_API + [
    ':its-base__plugin'
  ],
)

genrule(
  name = 'all',
  cmd = 'echo done >$OUT',
  deps = [
    ':its-base',
    ':its-base-javadoc',
    ':its-base-sources',
  ],
  out = '__fake.its-base__',
)

java_test(
  name = 'its-base_tests',
  srcs = glob(
    ['src/test/java/**/*.java'],
    excludes = TEST_UTIL_SRC
  ),
  labels = ['its-base'],
  source_under_test = [':its-base__plugin'],
  deps = GERRIT_PLUGIN_API + GERRIT_TESTS + [
    ':its-base__plugin',
    ':its-base_tests-utils',
  ],
)

if STANDALONE_MODE:
  include_defs('//VERSION')
  URL = 'https://oss.sonatype.org/content/repositories/snapshots' \
      if PLUGIN_VERSION.endswith('-SNAPSHOT') else \
        'https://oss.sonatype.org/service/local/staging/deploy/maven2'

  maven_package(
    repository = 'sonatype-nexus-staging',
    url = URL,
    version = PLUGIN_VERSION,
    group = 'com.googlesource.gerrit.plugins',
    jar = {'its-base': ':its-base__plugin'},
    src = {'its-base': ':its-base-sources'},
    doc = {'its-base': ':its-base-javadoc'},
  )

