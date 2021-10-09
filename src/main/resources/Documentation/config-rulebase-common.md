# Rule base configuration

[TOC]: # "Table of Contents"

### Table of Contents

- [Overview](#overview)
- [Rule Bases Scope](#rule-bases-scope)
- [Rules](#rules)
- [Conditions](#conditions)
- [Event Properties](#event-properties)
- [Actions](#actions)

## Overview

In this part we describe how to specify which events in Gerrit (E.g.:
“Change Merged”, or “User ‘John Doe’ voted ‘+2’ for ‘Code-Review’ on a
change”) trigger which actions (e.g.: “Set issue's status to
‘Resolved’”) on the ITS.

Actions on the ITS and conditions for the action to take place are
configured through rule bases.  A rule base is a git config file and
may contain an arbitrary number of rules. Each rule can have an
arbitrary number of conditions and actions. A rule fires all associated
actions, once all of its conditions are met.

A simple rule bases file may look like

```ini
[rule "rule1"]
    event-type = change-merged
    action = add-standard-comment
[rule "rule2"]
    event-type = comment-added
    approvalCodeReview = -2,-1
    action = add-comment Oh my Goodness! Someone gave a negative code review in Gerrit on an associated change.
```

This snippet defines two rules (`rule1`, and `rule2`). On merging a
change that's associated to some issues, `rule1` adds a predefined
standard comment for “Change Merged” to each such issue. If someone
adds a comment to a change that is associated to some issues and votes
“-2”, or “-1” for “Code-Review”, `rule2` adds the comment “Oh my
Goodness! Someone gave a negative code review in Gerrit on an
associated change.” to each such issue.

The order of rules in a rule base file need not be respected. So in the
above example, do not rely on `rule1` being evaluated before `rule2`.

## Rule bases scope

Rule bases can be defined in two scopes:

* Global
* Plugin specific

Global rules are picked up by all configured ITS plugins and they are
defined in a rule base file named `actions.config`.

Plugin specific rules are picked up only by @PLUGIN@ plugin and they
are defined in a rule base file named `actions-@PLUGIN@.config`.

A second aspect of rules bases scope is what projects they apply to;
in this case the rules can be:

* Generic
* Project specific

The generic scope refers to rules that apply to all projects that
enable ITS integration on the gerrit site; they are defined on rule
base files located inside the `gerrit_site/etc/its/` folder.

Project-specific rules are defined on rule base files located on the
`refs/meta/config` branch of a project and they apply exclusively to
the project that defines them (or that inherits them) and, possibly,
to child projects (see further explanations about rules inheritance
below). Project specific rules take precedence over generic rules,
i.e, when they are defined, generic rules do not apply to the project.

Thus, to define global generic rules, i.e., rules that are picked up
by all the ITS plugins and that apply to all the projects that enable
any ITS integration, they should be defined in the
`gerrit_site/etc/its/actions.config` rule base file.

Rules defined in the `gerrit_site/etc/its/actions-@PLUGIN@.config` rule
base file have generic but plugin specific scope, i.e., they apply to
all projects on the gerrit site that enable integration with @PLUGIN@.

On the other hand, if the rule base file `actions.config` is created
on the `refs/meta/config` branch of project 'P', the rules defined
on this file will have global but project specific scope, i.e, they
apply to all the ITS integrations defined for this project. Thus, if
project 'P' integrates with ITS system 'x' and with ITS system 'y',
the rules are applied to these two ITS integrations.

Contrarily, rules defined on the rule base file `actions-@PLUGIN@.config`
created on the `refs/meta/config` branch of project 'P' have project
and plugin specific scope, i.e., they apply only to the @PLUGIN@
integration defined for project 'P'.

Finally, is important to notice that if global and plugin specific rules
are defined, the final set of rules applied is the merge of them and this
is true either if they are defined in generic or project specific scope.

[rules-inheritance]: #rules-inheritance

### <a name="rules-inheritance">Rules inheritance</a>

For project specific rules, i.e., those defined on the `refs/meta/config`
branch of a project, inheritance is honored, similar to what is done in
other cases of project configurations.

Thus, if project 'P' defines project specific rules, these are applied
to children projects of project 'P' that enable an ITS integration.

This inheritance, however, is capped at the closest level, which means
that if a project defines at least one of the rule bases files,
(`actions.config` or `actions-@PLUGIN@.config`), the presence of these
files is not evaluated for any of the project's parents.

Thence, and continuing the example, if project 'Q' is a child of
project 'P' and project 'Q' enables ITS integration, rules defined in
project 'P' apply to project 'Q'. If however, project 'Q' defines its
own set of rules, either on file `actions.config` or
`actions-@PLUGIN@.config` (or both), the rules defined by project 'P'
no longer apply to project 'Q' , i.e., rules defined in project 'Q'
override and replace any rules defined on any parent project.

The same applies to the rules defined at the site level: if any project
defines their own rule base files, the global ones defined in the
`gerrit_site/etc/its/` folder do not apply to this project.

## Rules

Each rule consists of three items: A name, a set of conditions, and a
set of actions.

The rule's name (`rule1`, and `rule2` in the above example) is
currently not used and only provided for convenience.

For each rule the option `action` is interpreted as action. Any other
option of a rule is considered a condition.

Each of a rule's actions is taken for events that meet all of a
rule's conditions. If a rule contains more than one action
specifications, the order in which they are given need not be
respected.

There is no upper limit on the number of elements in a rules set of
conditions, and set of actions. Each of those sets may be empty.

## Conditions

The conditions are lines of the form

```ini
  name = value1, value2, ..., valueN
```

and match (if 'value1' is not `!`), if the event comes with a property
'name' having 'value1', or 'value2', or ..., or 'valueN'. So for
example to match events that come with an `association` property
having `subject`, or `footer-Bug`, the following condition can be
used:

```ini
  association = subject,footer-Bug
```

If 'value1' is `!`, the conditon matches if the event does not come
with a property 'name' having 'value2', or ..., or 'valueN'. So for
example to match events that do not come with a `status` property
having `DRAFT`, the following condition can be used:

```ini
  status = !,DRAFT
```

## Event Properties

The properties exposed by events depend on the kind of event.

For all events, the event's class name is provided in the `event`
property. Most native Gerrit events provide the `event-type`
property. So `event-type` (or `event` for other events fired by
plugins) allows you to write filters that fire only for a certain type
of event.

The common properties for each event are

`event`
: The event's class name.

`issue`
: Issue to which this event is associated. Each event is associated to
  exactly one issue. If for example an event is fired for a commit
  message, that would contain more than one issue id (say issue “23”,
  and issue “47”), then the event is duplicated and sent once for each
  associated issue (i.e.: once with `issue` being `23`, and once with
  `issue` being `47`).

`association`
: How the issue of property `issue` got associated to this event.
  See [Property: `association`][property-association].

`its-name`
:   Name of this plugin (i.e.: `@PLUGIN@`). This property can be used to
    make a rule in the rulebase match only for certain ITS plugins, if more
    than one is installed.

For example

```ini
    [rule "someRuleForBugzillaOnly"]
      its-name = its-bugzilla
      approvalCodeReview = -2
      action = add-comment Heya Bugzilla users, the change had a -2 Code-Review approval.
    [rule "someRuleForJiraOnly"]
      its-name = its-jira
      approvalCodeReview = -2
      action = add-comment Dear JIRA users, the change had a -2 Code-Review approval.
```

would report the “Heya Bugzilla...” text only through its-bugzilla for
changes that had a -2 Code-Review and have an association through
its-bugzilla. And for changes that had a -2 Code-Review and have an
association through its-jira, its-jira would report “Dear Jira users, ...”.

The further properties are listed in the event's
corresponding subsection below:

* [Property: `association`](#property-association)
* [ChangeAbandonedEvent](#changeabandonedevent)
* [ChangeMergedEvent](#changemergedevent)
* [ChangeRestoredEvent](#changerestoredevent)
* [CommentAddedEvent](#commentaddedevent)
* [PatchSetCreatedEvent](#patchSetcreatedevent)
* [RefUpdatedEvent](#refupdatedevent)
* [WorkInProgressStateChangedEvent](#workinprogressstatechangedevent)
* [PrivateStateChangedEvent](#privatestatechangedevent)
* [Common properties for events on a change](#common-properties-for-events-on-a-change)
* [Common properties for events on a patch set](#common-properties-for-events-on-a-patch-set)

### Property: `association`

The property `association` describes how the `issue` got associated to
this event.

An event typically has several `association` properties. Possible
values are:

`somewhere`
:	issue id occurs somewhere in the commit message of the change/the
	most recent patch set.

`subject`
:	issue id occurs in the first line of the commit message of the
	change/the most recent patch set.

`body`
:	issue id occurs after the subject but before the footer of the
	commit message of the change/the most recent patch set.

`footer`
:	issue id occurs in the last paragraph after the subject of the
	commit message of the change/the most recent patch set

`footer-<Key>`
:	issue id occurs in the footer of the commit message of the
	change/the most recent patch set, and is in a line with a key
	(part before the colon).

So for example, if the footer would contain a line

```
Fixes-Issue: issue 4711
```

then a property `association` with value `footer-Fixes-Issue`
would get added to the event for issue “4711”.

`added@<Association-Value>`
:	(only for events that allow to determine the patch set number.
	So for example, this `association` property is not set for
	RefUpdatedEvents)

issue id occurs at `<Association-Value>` in the most recent
patch set of the change, and either the event is for patch set
1 or the issue id does not occur at `<Association-Value>` in
the previous patch set.

So for example if issue “4711” occurs in the subject of patch
set 3 (the most recent patch set) of a change, but not in
patch set 2.  When adding a comment to this change, the event
for issue “4711” would get a property 'association' with value
`added@subject`.

### ChangeAbandonedEvent

`abandonerEmail`
: email address of the user abandoning the change.

`abandonerName`
: name of the user abandoning the change.

`abandonerUsername`
: username of the user abandoning the change.

`event`
: `com.google.gerrit.server.events.ChangeAbandonedEvent`

`event-type`
: `change-abandoned`

`reason`
: reason why the change has been abandoned.

In addition to the above properties, the event also provides
properties for the abandoned [Change][common-properties-for-events-on-a-change], and
its most recent [Patch Set][common-properties-for-events-on-a-patch-set].

### ChangeMergedEvent

`event`
: `com.google.gerrit.server.events.ChangeMergedEvent`

`event-type`
: `change-merged`

`submitterEmail`
: email address of the user causing the merge of the change.

`submitterName`
: name of the user causing the merge of the change.

`submitterUsername`
: username of the user causing the merge of the change.

In addition to the above properties, the event also provides
properties for the merged [Change][common-properties-for-events-on-a-change], and its
most recent [Patch Set][common-properties-for-events-on-a-patch-set].

### ChangeRestoredEvent

`event`
: `com.google.gerrit.server.events.ChangeRestoredEvent`

`event-type`
: `change-restored`

`reason`
: reason why the change has been restored.

`restorerEmail`
: email address of the user restoring the change.

`restorerName`
:  name of the user restoring the change.

`restorerUsername`
: username of the user restoring the change.

In addition to the above properties, the event also provides
properties for the restored [Change][common-properties-for-events-on-a-change], and it's
most recent [Patch Set][common-properties-for-events-on-a-patch-set].

### CommentAddedEvent

NOTE: For consistency with the other events, the `author-...`
properties of the CommentAddedEvent do not refer to the author of the
comment, but refer to the author of the change's latest patch set. The
author of the comment is accessible via the `commenter-...`
properties.

`commenterEmail`
: email address of the comment's author.

`commenterName`
: name of the comment's author.

`commenterUsername`
: username of the comment's author.

`comment`
: added comment itself.

`event`
: `com.google.gerrit.server.events.CommentAddedEvent+`

`event-type`
: `comment-added`

For each new or changed approval that has been made for this change, a
property of key `approval<LabelName>` and the approval's value as
value is added. So for example voting “-2” for the approval
“Code-Review” would add the following property:

`approvalCodeReview`
: `-2`

In addition to the above properties, the event also provides
properties for the [Change][common-properties-for-events-on-a-change] the comment was
added for, and it's most recent [Patch Set][common-properties-for-events-on-a-patch-set].

### PatchSetCreatedEvent

`event`
: `com.google.gerrit.server.events.PatchSetCreatedEvent`

`event-type`
: `patchset-created`

In addition to the above properties, the event also provides
properties for the uploaded [Patch Set][common-properties-for-events-on-a-patch-set],
and the [Change][common-properties-for-events-on-a-change] it belongs to.

### RefUpdatedEvent

`event`
: `com.google.gerrit.server.events.RefUpdatedEvent`

`event-type`
: `ref-updated`

`project`
: full name of the project from which a ref was updated.

`ref`
: git ref that has been updated (Typically the branch, as for example
  `refs/heads/master`).

`refSuffix`
: short name of the git ref that has been updated (Typically the branch or the
tag, as for example `master`).

`refPrefix`
: prefix of the git ref that has been updated (Example for a branch `refs/heads/`).

`revision`
: git commit hash the rev is pointing to now.

`revisionOld`
: git commit hash the rev was pointing to before.

`submitterEmail`
: email address of the user that updated the ref.

`submitterName`
: name of the user that updated the ref.

`submitterUsername`
: username of the user that updated the ref.

### WorkInProgressStateChangedEvent

`event`
:   `com.google.gerrit.server.events.WorkInProgressStateChangedEvent`

`event-type`
:   `wip-state-changed`

`changerEmail`
:   email address of the user that changed the WIP state

`changerName`
:   name of the user that changed the WIP state

`changerUsername`
:   username of the user that changed the WIP state

### PrivateStateChangedEvent

`event`
:   `com.google.gerrit.server.events.PrivateStateChangedEvent`

`event-type`
:   `private-state-changed`

`changerEmail`
:   email address of the user that changed the private state

`changerName`
:   name of the user that changed the private state

`changerUsername`
:   username of the user that changed the private state

### Common properties for events on a change

`branch`
: name of the branch the change belongs to.

`changeId`
: Change-Id for the change („I-followed by 40 hex digits” string).

`changeNumber`
: number for the change (plain integer).

`changeUrl`
: url of the change.

`formatChangeUrl`
: format the url for changeUrl.

`ownerEmail`
: email address of the change's owner.

`ownerName`
: name of the change's owner.

`ownerUsername`
: username of the change's owner.

`project`
: full name of the project the change belongs to.

`subject`
: first line of the change's most recent patch set's commit message.

`commitMessage`
: full commit message of the most recent patch set

`status`
:	status of the change (`null`, `NEW`, `SUBMITTED`, `MERGED`,
	or `ABANDONED` )

`topic`
: name of the topic the change belongs to.

`private`
:   whether the change is marked private

`wip`
:   whether the change is marked work in progress (WIP)

### Common properties for events on a patch set

`authorEmail`
: email address of this patch set's author.

`authorName`
: name of this patch set's author.

`authorUsername`
: username of this patch set's author.

`created-on`
: Timestamp of creation of the patch set (Seconds since 1st January 1970).

`deletions`
: number of lines deleted by the patch set.

`insertions`
: number of lines inserted by the patch set.

`parents`
: A list of git commit hashes that are parents to the patch set.

`patchSetNumber`
: patch set's number within the change.

`ref`
: git ref for the patch set (For the 5-th patch set of change 4711, this
  will be `refs/changes/11/4711/5`).

`revision`
: git commit hash of the patch set

`uploaderEmail`
: email address of the user that uploaded this patch set.

`uploaderName`
: name of the user that uploaded this patch set.

`uploaderUsername`
: username of the user that uploaded this patch set.

### Common properties for all events

`source`
: source of the event that triggered the action. Can be `its` or `gerrit`.
  `its` sourced events are fired by its actions. e.g. `fire-event-on-commits` action fires `its` sourced events.
  `gerrit` sourced events are directly fired by the Gerrit event system.

## Actions

Lines of the form

```ini
  action = name param1 param2 ... paramN
```

represent the action `name` being called with parameters `param1`,
`param2`, ... `paramN`.

The following actions are available:

[`add-comment`][action-add-comment]
: adds the parameters as issue comment

[`add-standard-comment`][action-add-standard-comment]
: adds a predefined standard comment for certain events

[`add-soy-comment`][action-add-soy-comment]
: adds a rendered Closure Template (soy) template as issue comment

[`create-version-from-property`][action-create-version-from-property]
: creates a version based on an event's property value

[`log-event`][action-log-event]
: appends the event's properties to Gerrit's log

[Further actions][further-actions] may be provided by @PLUGIN@.

[further-actions]: config-rulebase-plugin-actions.md

### Action: add-comment

The `add-comment` action adds the given parameters as comment to any
associated rule.

So for example

```ini
  action = add-comment This is a sample command
```

would add a comment “This is a sample command” to associated issues.

If no parameters are given, no comment gets added.

### Action: add-standard-comment

The `add-standard-comment` action adds predefined comments to
associated issues for change abandoned, merged, restored, and patch
set created events. For other events, no comment is added to the
associated issues.

The added comments contain the person responsible for the event
(abandoner, merger, ...), the change's subject, a reason (if one has
been given), and a link to the change.

### Action: add-soy-comment

The `add-soy-comment` action renders a Closure template (soy) for the
event and adds the output as comment to any associated issue.

So for example

```ini
  action = add-soy-comment TemplateName
```

would render the template `etc/its/templates/TemplateName.soy` add the
output as comment to associated issues.

example for what the soy template will look like (note @param is required with correct variables.)

```
{namespace etc.its.templates}
{template .TemplateName kind="text"}
  {@param changeNumber: string}
  {@param formatChangeUrl: string}
  Comment for change {$changeNumber} added. See {$formatChangeUrl}
{/template}
```

Any [property][event-properties] of the event may be used from
templates. So for example `$subject` in the above example refers to
the event's subject property, and `$changeNumber` would refer to the
change's number.

### Action: add-property-to-field

The `add-property-to-field` action adds an event property value to an ITS designated field.

The field is expected to be able to hold multiple values.
The ITS field value deduplication depends on the its implementation.

Example with the event property `branch` and a field identified as `labels`:

```ini
  action = add-property-to-field branch labels
```

### Action: create-version-from-property

The `create-version-from-property` action creates a version in the ITS project
by using an event property value as the version value.

This is useful when you want to create a new version in the ITS when a tag is
created in the Gerrit project.

Example with the event property `ref`:

```ini
  action = create-version-from-property ref
```

### Action: fire-event-on-commits

The `fire-event-on-commits` action start by collecting commits using a collector then fire the event
on each collected commit.

This is useful when you want to trigger rules on a multiple past commits.

Available collectors are:

- `since-last-tag`: Collects all commits between the current ref and previous tag

To avoid to trigger issue actions twice for the same event, you should condition your rule on
the event property `source`.

Example:

```ini
  action = fire-event-on-commits since-last-tag
```

### Action: log-event

The `log-event` action appends the event's properties to Gerrit's log.

Logging happens at the info level per default, but can be overriden by
adding the desired log level as parameter. Supported values are
`error`, `warn`, `info`, and `debug`). So for example

```ini
  action = log-event error
```

appends the event's properties to Gerrit's log at error level. All
other parameters are ignored.

This action is useful, when testing rules or trying to refine
conditions on rules, as it make the available properties visible.

[Back to @PLUGIN@ documentation index][index]

[index]: index.html
