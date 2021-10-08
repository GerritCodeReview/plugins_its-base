# Common configuration for `its-base`-based plugins

[TOC]: # "Table of Contents"

### Table of Contents
- [Identifying ITS ids](#identifying-its-ids)
- [Enabling ITS integration](#enabling-its-integration)
- [Associating a Gerrit project with its ITS project counterpart](#associating-a-gerrit-project-with-its-its-project-counterpart)
- [Configuring rules of when to take which actions in the ITS](#configuring-rules-of-when-to-take-which-actions-in-the-its)
- [Multiple Its](#multiple-its)
- [Further common configuration details](#further-common-configuration-details)


## Identifying ITS ids

In order to extract ITS ids from commit messages, @PLUGIN@ uses
[commentlink][upstream-comment-link-doc]s of
([per default][common-config-commentlink]) name "`@PLUGIN@`".

The ([per default][common-config-commentlinkGroupIndex]) first group of
`commentlink.@PLUGIN@.match` is considered the issue id.

So for example having

```ini
[commentlink "@PLUGIN@"]
    match = [Bb][Uu][Gg][ ]*([1-9][0-9]*)
    html = "<a href=\"http://my.issure.tracker.example.org/show_bug.cgi?id=$1\">(bug $1)</a>"
```

in `etc/gerrit.config` would allow to match the issues `4711`, `167`
from a commit message like

```
Sample commit message relating to bug 4711, and bug 167.
```

[upstream-comment-link-doc](../../../Documentation/config-gerrit.html#commentlink)

By setting a `commentlink`'s `association` on the plugin's @PLUGIN@ configuration, it
is possible to require commits to carry ITS references; the following
values are supported (default is `OPTIONAL`):

MANDATORY
:	 One or more issue-ids are required in the git commit message.  The git push will
	 be rejected otherwise. Note that in case of connectivity issues with ITS,
	 the commit will be accepted. The client will be notified about the
	 connectivity issue and the result.

SUGGESTED
:	 Whenever the git commit message does not contain one or more issue-ids,
	 a warning message is displayed as a suggestion on the client.

OPTIONAL
:	 Bug-ids are liked when found in the git commit message, no warning is
	 displayed otherwise.

Example:

```ini
[plugin "@PLUGIN@"]
    association = MANDATORY
```

in `etc/gerrit.config` would accept only commits that contain a valid issue id
in the comment, matching the commentLink defined previously.

NOTE: Historically the association has been defined in the Gerrit's commentLink
section. That setting is deprecated but still supported for the current release.
You are encouraged to move the association policy to the plugin section, the
commentLink.association will be discontinued in the next major release.

The association can be overridden at project level in the project.config
using the same syntax used in the gerrit.config. Project's hierarchy will be respected
when evaluating the links configuration and association policy.

## Enabling ITS integration

It can be configured per project whether the issue tracker
integration is enabled or not. To enable the issue tracker integration
for a project the project must have the following entry in its
`project.config` file in the `refs/meta/config` branch:

```ini
  [plugin "@PLUGIN@"]
    enabled = true
```

If `plugin.@PLUGIN@.enabled` is not specified in the `project.config`
file the value is inherited from the parent project. If it is not
set on any parent project the issue integration is disabled for this
project.

By setting `plugin.@PLUGIN@.enabled` to true in the `project.config`
of the `All-Projects` project the issue tracker integration can be
enabled by default for all projects. During the initialization of the
plugin you are asked if the issue integration should be enabled by
default for all projects and if yes this setting in the
`project.config` of the `All-Projects` project is done automatically.

With this it is possible to support integration with multiple
issue tracker systems on a server. E.g. a project can choose if it
wants to enable integration with Jira or with Bugzilla.

If child projects must not be allowed to disable the issue tracker
system integration a project can enforce the issue tracker system
integration for all child projects by setting
`plugin.@PLUGIN@.enabled` to `enforced`.

The issue tracker system integration can be limited to specific
branches by setting `plugin.@PLUGIN@.branch`. The branches may be
configured using explicit ref names, ref patterns, or regular
expressions. Multiple branches may be specified.

E.g. to limit the issue tracker system integration to the `master`
branch and all stable branches the following could be configured:

```ini
  [plugin "@PLUGIN@"]
    enabled = true
    branch = refs/heads/master
    branch = ^refs/heads/stable-.*
```

## Associating a Gerrit project with its ITS project counterpart

To be able to make use of actions acting at the ITS project level, you must
associate a Gerrit project to its ITS project counterpart.

It must be configured per project and per plugin. To configure the association
for a project mapping to an ITS project named `manhattan-project`, the project
must have the following entry in its `project.config` file in the
`refs/meta/config` branch:

```ini
  [plugin "@PLUGIN@"]
    its-project = manhattan-project
```

## Configuring rules of when to take which actions in the ITS

Setting up which event in Gerrit (E.g.: “Change Merged”, or “User
‘John Doe’ voted ‘+2’ for ‘Code-Review’ on a change”) should trigger
which action on the ITS (e.g.: “Set issue's status to ‘Resolved’”) is
configured through a [rule base][rule-base] in
`etc/its/actions.config`.

[rule-base]: config-rulebase-common.md



## Multiple ITS

Although not a common setup the @PLUGIN@ plugin supports connecting
Gerrit to multiple issue tracking systems.

For example users may want to reference issues from two independent
issue tracking systems (i.e. a Bugzilla and a Jira instance).  In
this configuration you can simply install both its plugins and
configure them as described.

In situations where users want to reference issues from multiple instances of
the same issue tracking system (e.g., two independent Bugzilla instances),
create two its-bugzilla plugin files with different names (e.g.,
`its-bugzilla-external.jar` and `its-bugzilla-internal.jar`). Edit the file
`META-INF\MANIFEST.MF` in the jar (extract using `unzip`), and change the name
in the `Gerrit-PluginName` field to match the change to the filename. Now you
just need to use the appropriate name to configure each plugin.

## Further common configuration details

[common-config-commentlink](#common-config-commentlink)
[common-config-commentlinkGroupIndex](#common-config-commentlinkGroupIndex)

<a name="common-config-commentlink">`@PLUGIN@.commentlink`</a>
:   The name of the comment link to use to extract issue ids.

    This setting is useful to reuse the same comment link from different Its
    plugins. For example, if you set `@PLUGIN@.commentlink` to `foo`, then the
    comment link `foo` is used (instead of the comment link `@PLUGIN@`) to
    extract issue ids.

    Default is `@PLUGIN@`

<a name="common-config-commentlinkGroupIndex">`@PLUGIN@.commentlinkGroupIndex`</a>
:   The group index within `@PLUGIN@.commentlink` that holds the issue id.

    Default is `1`, if there are are groups within the regular expression for
    the `@PLUGIN@.commentlink` comment link, and the default is `0`, if there
    are no such groups.

<a name="common-config-dummyIssuePattern">`@PLUGIN@.dummyIssuePattern`</a>
:   Pattern which can be specified to match a dummy issue.

    This setting is useful to bypass the MANDATORY check for commits matching
    a specific pattern.

[Back to @PLUGIN@ documentation index][index]

[index]: index.html
