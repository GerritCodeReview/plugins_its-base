DEPS = [
  '//gerrit-plugin-api:lib',
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
  provided_deps = DEPS,
)

java_test(
  name = 'its-base_tests',
  srcs = glob(['src/test/java/**/*.java']),
  labels = ['its-base'],
  source_under_test = [':its-base__plugin'],
  deps = DEPS + [
    ':its-base__plugin',
    '//lib:easymock',
    '//lib/log:impl_log4j',
    '//lib/log:log4j',
    '//lib:junit',
    '//lib:powermock-api-easymock',
    '//lib:powermock-api-support',
    '//lib:powermock-core',
    '//lib:powermock-module-junit4',
    '//lib:powermock-module-junit4-common',
  ],
)
