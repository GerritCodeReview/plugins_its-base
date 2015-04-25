Build
=====

This base library for ITS-based plugins is built with Buck.

Clone or link this plugin to the plugins directory of Gerrit's source
tree, and issue the command:

```
  buck build plugins/its-base
```

The output is created in

```
  buck-out/gen/plugins/its-base/its-base.jar
  buck-out/gen/plugins/its-base/lib__its-base__plugin__output/its-base__plugin.jar
```

This project can be imported into the Eclipse IDE:

```
  ./tools/eclipse/project.py
```

To execute the tests run:

```
  buck test --all --include its-base
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