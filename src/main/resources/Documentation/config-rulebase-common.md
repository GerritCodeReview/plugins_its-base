Rule base configuration
=======================

#### Table of Contents
* [Overview][overview]
* [Rules][rules]
* [Conditions][conditions]
* [Event Properties][event-properties]
* [Actions][actions]

[overview]: #overview
<a name="overview">Overview</a>
-------------------------------

In this part we describe how to specify which events in Gerrit (E.g.:
“Change Merged”, or “User ‘John Doe’ voted ‘+2’ for ‘Code-Review’ on a
change”) trigger which actions (e.g.: “Set issue's status to
‘Resolved’”) on the ITS.

Actions on the ITS and conditions for the action to take place are
configured through the rule bases in `etc/its/actions.config` (for global rules
to be picked up by all configured ITS plugins) and
`etc/its/actions-@PLUGIN@.config` (for rules to be picked up only by @PLUGIN@)
in the site directory. A rule base is a git config file, and may contain an
arbitrary number of rules. Each rule can have an arbitrary number of conditions
and actions. A rule fires all associated actions, once all of its conditions are
met.

A simple `etc/its/actions.config` (or
`etc/its/actions-@PLUGIN@.config`) may look like

```
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

The order of rules in `etc/its/actions.config` need not be
respected. So in the above example, do not rely on `rule1` being
evaluated before `rule2`.

[rules]: #rules
<a name="rules">Rules</a>
-------------------------

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

[conditions]: #conditions
<a name="conditions">Conditions</a>
-----------------------------------

The conditions are lines of the form

```
  name = value1, value2, ..., valueN
```

and match (if 'value1' is not `!`), if the event comes with a property
'name' having 'value1', or 'value2', or ..., or 'valueN'. So for
example to match events that come with an `association` property
having `subject`, or `footer-Bug`, the following condition can be
used:

```
  association = subject,footer-Bug
```

If 'value1' is `!`, the conditon matches if the event does not come
with a property 'name' having 'value2', or ..., or 'valueN'. So for
example to match events that do not come with a `status` property
having `DRAFT`, the following condition can be used:

```
  status = !,DRAFT
```

[event-properties]: #event-properties
<a name="event-properties">Event Properties</a>
-----------------------------------------------

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

    ```
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

* [ChangeAbandonedEvent][event-properties-ChangeAbandonedEvent]
* [ChangeMergedEvent][event-properties-ChangeMergedEvent]
* [ChangeRestoredEvent][event-properties-ChangeRestoredEvent]
* [CommentAddedEvent][event-properties-CommentAddedEvent]
* [PatchSetCreatedEvent][event-properties-PatchSetCreatedEvent]
* [RefUpdatedEvent][event-properties-RefUpdatedEvent]
* [Common properties for events on a change][event-properties-change]
* [Common properties for events on a patch set][event-properties-patch-set]

[property-association]: #property-association
### <a name="property-association">Property: `association`</a>

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
	RevUpdatedEvents)

issue id occurs at `<Association-Value>` in the most recent
patch set of the change, and either the event is for patch set
1 or the issue id does not occur at `<Association-Value>` in
the previous patch set.

So for example if issue “4711” occurs in the subject of patch
set 3 (the most recent patch set) of a change, but not in
patch set 2.  When adding a comment to this change, the event
for issue “4711” would get a property 'association' with value
`added@subject`.

[event-properties-ChangeAbandonedEvent]: #event-properties-ChangeAbandonedEvent
### <a name="event-properties-ChangeAbandonedEvent">ChangeAbandonedEvent</a>

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
properties for the abandoned [Change][event-properties-change], and
its most recent [Patch Set][event-properties-patch-set].

[event-properties-ChangeMergedEvent]: #event-properties-ChangeMergedEvent
### <a name="event-properties-ChangeMergedEvent">ChangeMergedEvent</a>

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
properties for the merged [Change][event-properties-change], and its
most recent [Patch Set][event-properties-patch-set].

[event-properties-ChangeRestoredEvent]: #event-properties-ChangeRestoredEvent
### <a name="event-properties-ChangeRestoredEvent">ChangeRestoredEvent</a>

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
properties for the restored [Change][event-properties-change], and it's
most recent [Patch Set][event-properties-patch-set].

[event-properties-CommentAddedEvent]: #event-properties-CommentAddedEvent
### <a name="event-properties-CommentAddedEvent">CommentAddedEvent</a>

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
: `com.google.gerrit.server.events.CommentAddedEvent+

`event-type`
: `comment-added`

For each new or changed approval that has been made for this change, a
property of key `approval<LabelName>` and the approval's value as
value is added. So for example voting “-2” for the approval
“Code-Review” would add the following property:

`approvalCodeReview`
: `-2`

In addition to the above properties, the event also provides
properties for the [Change][event-properties-change] the comment was
added for, and it's most recent [Patch Set][event-properties-patch-set].

[event-properties-PatchSetCreatedEvent]: #event-properties-PatchSetCreatedEvent
### <a name="event-properties-PatchSetCreatedEvent">PatchSetCreatedEvent</a>

`event`
: `com.google.gerrit.server.events.PatchSetCreatedEvent`

`event-type`
: `patchset-created`

In addition to the above properties, the event also provides
properties for the uploaded [Patch Set][event-properties-patch-set],
and the [Change][event-properties-change] it belongs to.

[event-properties-RefUpdatedEvent]: #event-properties-RefUpdatedEvent
### <a name="event-properties-RefUpdatedEvent">RefUpdatedEvent</a>

`event`
: `com.google.gerrit.server.events.RefUpdatedEvent`

`event-type`
: `ref-updated`

`project`
: full name of the project from which a ref was updated.

`ref`
: git ref that has been updated (Typcially the branch, as for example
  `master`).

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

[event-properties-change]: #event-properties-change
### <a name="event-properties-change">Common properties for events on a change</a>

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

[event-properties-patch-set]: #event-properties-patch-set
### <a name="event-properties-patch-set">Common properties for events on a patch set</a>

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

[event-properties-all]: #event-properties-all
### <a name="event-properties-all">Common properties for all events</a>

`event-origin`
: origin of the event that triggered the action. Can be `issue` or `project`.

[actions]: #actions
<a name="actions">Actions</a>
-----------------------------

Lines of the form

```
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

[action-add-comment]: #action-add-comment
### <a name="action-add-comment">Action: add-comment</a>

The `add-comment` action adds the given parameters as comment to any
associated rule.

So for example

```
  action = add-comment This is a sample command
```

would add a comment “This is a sample command” to associated issues.

If no parameters are given, no comment gets added.

[action-add-standard-comment]: #action-add-standard-comment
### <a name="action-add-standard-comment">Action: add-standard-comment</a>

The `add-standard-comment` action adds predefined comments to
associated issues for change abandoned, merged, restored, and patch
set created events. For other events, no comment is added to the
associated issues.

The added comments contain the person responsible for the event
(abandoner, merger, ...), the change's subject, a reason (if one has
been given), and a link to the change.

[action-add-soy-comment]: #action-add-soy-comment
### <a name="action-add-soy-comment">Action: add-soy-comment</a>

The `add-soy-comment` action renders a Closure template (soy) for the
event and adds the output as comment to any associated issue.

So for example

```
  action = add-soy-comment TemplateName
```

would render the template `etc/its/templates/TemplateName.soy` add the
output as comment to associated issues.

example for what the soy template will look like (note @param is required with correct variables.)


```
{namespace etc.its.templates}

/**
 * @param changeNumber
 * @param formatChangeUrl
 */
{template .TemplateName autoescape="strict" kind="text"}
  inline Comment for change {$changeNumber} added. See {$formatChangeUrl}
{/template}
```

Any [property][event-properties] of the event may be used from
templates. So for example `$subject` in the above example refers to
the event's subject property, and `$changeNumber` would refer to the
change's number.

[action-create-version-from-property]: #create-version-from-property
### <a name="create-version-from-property">Action: create-version-from-property</a>

The `create-version-from-property` action creates a version in the ITS project by using an event 
property value as the version value.

This is useful when you want to create a new version in the ITS when a tag is created in the 
Gerrit project.

Example with the event property `ref`:

```
  action = create-version-from-property ref
```

[action-fire-event-on-commits]: #fire-event-on-commits
### <a name="fire-event-on-commits">Action: fire-event-on-commits</a>

The `fire-event-on-commits` action start by collecting commits using a collector then fire the event
on each collected commit.

This is useful when you want to trigger rules on a multiple past commits.

Available collectors are:
- `since-last-tag`: Collects all commits between the current ref and previous tag

To avoid to trigger issue actions twice for the same event, you should condition your rule on
the event property `event-origin`.

Example:

```
  action = fire-event-on-commits since-last-tag
```


[action-log-event]: #action-log-event
### <a name="action-log-event">Action: log-event</a>

The `log-event` action appends the event's properties to Gerrit's log.

Logging happens at the info level per default, but can be overriden by
adding the desired log level as parameter. Supported values are
`error`, `warn`, `info`, and `debug`). So for example

```
  action = log-event error
```

appends the event's properties to Gerrit's log at error level. All
other parameters are ignored.

This action is useful, when testing rules or trying to refine
conditions on rules, as it make the available properties visible.

[Back to @PLUGIN@ documentation index][index]

[index]: index.html
