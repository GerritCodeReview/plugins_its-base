Build
=====

This base library for ITS-based plugins is built with Buck.

Two build modes are supported: Standalone and in Gerrit tree. Standalone
build mode is recommended, as this mode doesn't require local Gerrit
tree to exist.

Build standalone
----------------

Clone bucklets library:

```
  git clone https://gerrit.googlesource.com/bucklets

```
and link it to its-base directory:

```
  cd its-base && ln -s ../bucklets .
```

Add link to the .buckversion file:

```
  cd its-base && ln -s bucklets/buckversion .buckversion
```

To build the plugin, issue the following command:


```
  buck build plugin
```

The output is created in

```
  buck-out/gen/its-base/its-base.jar
```

To execute the unit tests, issue the following command:

```
  buck test
```

To publish the artifact to the local Maven repository to consume from
dependent plugins, issue the following command:

```
  buck build in
```

Build in Gerrit tree
--------------------

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

Note that for compatibility reasons a Maven build is provided, but is
considered to be deprecated and will be removed in a future version of
this plugin.

To build with Maven, change directory to the plugin folder and issue the
command:

```
  mvn clean package
```

When building with Maven, the Gerrit Plugin API must be available.

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
