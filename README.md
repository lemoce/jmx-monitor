jmx-monitor
===========

It is small program for monitoring Java local process on S.O. . It was written in Scala using SBT.

Usage
-----

jmx-monitor ``<pid>` `[<mbean name> <attribute to monitor>]``

Output
------

Program writes csv line with all selected attributes each 1 second.