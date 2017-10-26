load("//tools/bzl:maven_jar.bzl", "maven_jar")

def external_plugin_deps():
  maven_jar(
    name = "velocity",
    artifact = "org.apache.velocity:velocity:1.7",
    sha1 = "2ceb567b8f3f21118ecdec129fe1271dbc09aa7a",
  )
