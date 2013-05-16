hooks-its
=========

link:https://gerrit-review.googlesource.com/#/admin/projects/plugins/hooks-its['hooks-its']
is by itself not a real plugin, but the common parent project for issue tracking
system (ITS) plugins for gerrit, such as
link:https://gerrit-review.googlesource.com/#/admin/projects/plugins/hooks-bugzilla['hooks-bugzilla'],
or
link:https://gerrit-review.googlesource.com/#/admin/projects/plugins/hooks-jira['hooks-jira'].



Common configuration
--------------------

The base functionality for 'hooks-its' based plugins can be configured via the
plugin's section (e.g.: `bugzilla` for 'hooks-bugzilla') within
`etc/gerrit.config`. In the following description, we use `itsName` as
placeholder for the plugin's name.  Be sure to replace it with the plugin's real
name in the documentation of a 'hooks-its' based plugin (e.g.: Use `bugzilla`
instead of `itsName` for 'hooks-bugzilla').

[[itsName.commentOnChangeAbandoned]]itsName.commentOnChangeAbandoned::
+
If true, abandoning a change adds an ITS comment to the change's associated
issue.
+
Default is `true`.

[[itsName.commentOnChangeCreated]]itsName.commentOnChangeCreated::
+
If true, creating a change adds an ITS comment to the change's associated issue.
+
Default is `false`.

[[itsName.commentOnChangeMerged]]itsName.commentOnChangeMerged::
+
If true, merging a change's patch set adds an ITS comment to the change's
associated issue.
+
Default is `true`.

[[itsName.commentOnChangeRestored]]itsName.commentOnChangeRestored::
+
If true, restoring an abandoned change adds an ITS comment to the change's
associated issue.
+
Default is `true`.

[[itsName.commentOnCommentAdded]]itsName.commentOnCommentAdded::
+
If true, adding a comment and/or review to a change in gerrit adds an ITS
comment to the change's associated issue.
+
Default is `true`.

[[itsName.commentOnFirstLinkedPatchSetCreated]]itsName.commentOnFirstLinkedPatchSetCreated::
+
If true, creating a patch set for a change adds an ITS comment to the change's
associated issue, if the issue has not been mentioned in previous patch sets of
the same change.
+
Default is `false`.

[[itsName.commentOnPatchSetCreated]]itsName.commentOnPatchSetCreated::
+
If true, creating a patch set for a change adds an ITS comment to the change's
associated issue.
+
Default is `true`.

[[itsName.commentOnRefUpdatedGitWeb]]itsName.commentOnRefUpdatedGitWeb::
+
If true, updating a ref adds a GitWeb link to the associated issue.
+
Default is `true`.
