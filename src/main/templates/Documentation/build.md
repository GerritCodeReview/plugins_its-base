Build
=====

This base library for ITS-based plugins is built with Bazel.

Two build modes are supported: Standalone and in Gerrit tree.
The standalone build mode is recommended, as this mode doesn't require
the Gerrit tree to exist locally.

### Build standalone

```
  bazel build its-base
```

The output is created in

```
  bazel-genfiles/its-base.jar
```

To execute the tests run:

```
  bazel test :its_base_tests
```

### Build in Gerrit tree

```
  bazel build plugins/its-base
```

The output is created in

```
  bazel-genfiles/plugins/its-base/its-base.jar
```

This project can be imported into the Eclipse IDE:

```
  ./tools/eclipse/project.py
```

To execute the tests run:

```
  bazel test plugins/its-base:its_base_tests
```

Note that the ITS-based plugins require `its-base__plugin` library:

```
[...]
  deps = [
    '//plugins/its-base:its-base__plugin',
  ],
[...]
```

How to build the Gerrit Plugin API is described in the [Gerrit
documentation](../../../Documentation/dev-buck.html#_extension_and_plugin_api_jar_files).

[Back to @PLUGIN@ documentation index][index]

[index]: index.html
