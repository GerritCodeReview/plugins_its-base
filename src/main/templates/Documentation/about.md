its-base
========

`its-base` is a common stub for Gerrit plugins that connect to issue
tracking systems ("ITS"). `its-base` on its own is not meant to be
installed as plugin to a Gerrit site. Instead `its-base` provides
common functionality to other plugins, like:

* [its-bugzilla][its-bugzilla]
* [its-jira][its-jira]
* [its-rtc][its-rtc]

[its-bugzilla]: https://gerrit-review.googlesource.com/#/admin/projects/plugins/its-bugzilla
[its-jira]: https://gerrit-review.googlesource.com/#/admin/projects/plugins/its-jira
[its-rtc]: https://gerrit-review.googlesource.com/#/admin/projects/plugins/its-rtc

`its-base` provides means to:

* Add comments to an ITS (based on a user-defined rules, like "Add a
  comment to the ITS, if a change references the respective issue").
* Change status of ITS entries (based on a user-defined rules, like
  "Set status to `resolved` if a change gets merged that references
  the respective issue").
* (De-)activate on per-project base.

<span></span>
