DevSync - Simple file sync utility
==================================

DevSync is a simple file syncing utility. It was designed specifically to sync
source code files on a laptop or similar with the filesystem inside a Docker
container or VM.

DevSync upon startup performs an inital file sync phase, and then enters a
file system watching phase, automatically copying file and directory changes
to the remote endpoint. It can replicate changes on your laptop to your
container/VM, and changes from your container/VM back to your laptop. A
configuration file is used to tel DevSync which files to copy and ignore.

# Example - Magento 2

A good example of where DevSync is useful is with Magento 2 development.

* In order to use PHP Storm, you get best performance if you keep the source
  code locally on your laptop.
* You might however also use Sublime or vim to make file system changes, so
  you cannot rely on PHP Storm's "push file on save" feature.
* So master source code files are pushed from the local file system into the
  container/VM on change.
  
* Magento 2 generates some source files - on demand, or during a compilation
  phase. In order to use PHP Storm for debugging, it is useful to have this
  generated code downloaded to your laptop. So you want to pull `var/generated`.
* You don't want to push or pull `var` otherwise since it is a scratch area.

* You don't want to push `.git` into the container - its just slow for no
  benefit. (In fact, there is risk doing so as you may accidentally try to run
  'git' commands inside the container.)
* You don't want to push any (large) Vagrant images as well. So there are a
  number of files you simply don't want to push.

It is this combination of pushing some directories, pulling other files, and
excluding certain files and directories which caused the reason for the creation
of DevSync.

The `sample.devsync.yml` file is an example `.devsync.yml` file that may be suitable
for Magento 2 development.

# Installation

This is a work in progress. The application is written in Java, but there are
tools to generate Windows and MacOS binaries with a simple installer. That would
make installation of this Java application much simpler for developers.

The intended setup instructions for a new user is going to be:

1. Install DevSync (single Windows and MacOS installer to download and run).
2. Install PHP Storm or vim. (Note PHP Storm installation does *not* require
   PHP to be installed locally.)
3. Start up a container/VM holding the web server, MySQL database, Grunt/Gulp,
   etc. The DevSync server would be preinstalled and running.
4. Start DevSync in a window in the project source code directory.

That is, tools such as Composer would be run inside the container/VM. PHP Storm
supports "remote interpreters" meaning you can single step programs without
PHP installed on your laptop.

# Why not use Rsync/Unison/fswatch/...

The reason for DevSync was a combination of the complexity of the configuration
file and portability. On Windows, rsync is available for Cygwin and MinWin,
but the different distributions use different path name conventions
(such as `/cygdrive/c/` vs `/c/`). If you were in the wrong window, things could
go wrong. DevSync will be a native executable, not dependent on installation
of cywgin etc for use.

But rsync is a robust and fully functional tool, with SSH support built in.

# Process Structure

DevSync works by having a permanently running server in your VM/container
listening for connections. You then start up a client on your laptop which
loads up a configuration file containing instructions of what to sync.

There is no security. No authentication, no channel encryption. It was designed
soley for a developer syncing files with a VM/container they are running
locally. If you want to sync to a cloud server, you could explore using a
SSH tunnel where SSH can then do all authentication and channel encryption.

There is a `.devsync.yml` file from which configuration settings are read. This
file can be committed into a GIT repository or similar.

The client locates the server using two environment variables, `DEVSYNC_HOST`
and `DEVSYNC_PORT`.

The server gets the port number to listen on from the command line if specified,
otherwise the `DEVSYNC_PORT` environment variable.
