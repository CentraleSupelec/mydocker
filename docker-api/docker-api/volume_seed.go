package main

import (
	"encoding/base64"
	"fmt"
)

// The volume-init seeds an empty student volume with these two files before the
// lab first mounts it. Docker copies the image's content at the mount point into
// a named volume only while the volume is empty, and gives the volume root the
// image's owner and mode. A seeded volume is no longer empty, so a new student
// never starts with a home owned by the image's user, and the copy of the
// image's caches disappears from the first start.
//
// The .profile is the stock Debian/Ubuntu skeleton, identical in every course
// image that ships one. The .bashrc is the skeleton as the Jupyter stack images
// ship it (colour prompt and ls aliases on); the micromamba images ship the same
// file with those lines commented out. The images end their .bashrc with one line
// that enters the course environment: the micromamba images source
// _activate_current_env.sh, the Jupyter stack images run the conda hook. The
// seeded .bashrc ends with a block that does whichever applies to the image the
// student runs, so an image that has conda on its PATH but never ran the hook now
// shows the base environment in the prompt.

const seedProfile = `# ~/.profile: executed by the command interpreter for login shells.
# This file is not read by bash(1), if ~/.bash_profile or ~/.bash_login
# exists.
# see /usr/share/doc/bash/examples/startup-files for examples.
# the files are located in the bash-doc package.

# the default umask is set in /etc/profile; for setting the umask
# for ssh logins, install and configure the libpam-umask package.
#umask 022

# if running bash
if [ -n "$BASH_VERSION" ]; then
    # include .bashrc if it exists
    if [ -f "$HOME/.bashrc" ]; then
	. "$HOME/.bashrc"
    fi
fi

# set PATH so it includes user's private bin if it exists
if [ -d "$HOME/bin" ] ; then
    PATH="$HOME/bin:$PATH"
fi

# set PATH so it includes user's private bin if it exists
if [ -d "$HOME/.local/bin" ] ; then
    PATH="$HOME/.local/bin:$PATH"
fi
`

const seedBashrc = `# ~/.bashrc: executed by bash(1) for non-login shells.
# see /usr/share/doc/bash/examples/startup-files (in the package bash-doc)
# for examples

# If not running interactively, don't do anything
case $- in
    *i*) ;;
      *) return;;
esac

# don't put duplicate lines or lines starting with space in the history.
# See bash(1) for more options
HISTCONTROL=ignoreboth

# append to the history file, don't overwrite it
shopt -s histappend

# for setting history length see HISTSIZE and HISTFILESIZE in bash(1)
HISTSIZE=1000
HISTFILESIZE=2000

# check the window size after each command and, if necessary,
# update the values of LINES and COLUMNS.
shopt -s checkwinsize

# If set, the pattern "**" used in a pathname expansion context will
# match all files and zero or more directories and subdirectories.
#shopt -s globstar

# make less more friendly for non-text input files, see lesspipe(1)
[ -x /usr/bin/lesspipe ] && eval "$(SHELL=/bin/sh lesspipe)"

# set variable identifying the chroot you work in (used in the prompt below)
if [ -z "${debian_chroot:-}" ] && [ -r /etc/debian_chroot ]; then
    debian_chroot=$(cat /etc/debian_chroot)
fi

# set a fancy prompt (non-color, unless we know we "want" color)
case "$TERM" in
    xterm-color|*-256color) color_prompt=yes;;
esac

# uncomment for a colored prompt, if the terminal has the capability; turned
# off by default to not distract the user: the focus in a terminal window
# should be on the output of commands, not on the prompt
force_color_prompt=yes

if [ -n "$force_color_prompt" ]; then
    if [ -x /usr/bin/tput ] && tput setaf 1 >&/dev/null; then
	# We have color support; assume it's compliant with Ecma-48
	# (ISO/IEC-6429). (Lack of such support is extremely rare, and such
	# a case would tend to support setf rather than setaf.)
	color_prompt=yes
    else
	color_prompt=
    fi
fi

if [ "$color_prompt" = yes ]; then
    PS1='${debian_chroot:+($debian_chroot)}\[\033[01;32m\]\u@\h\[\033[00m\]:\[\033[01;34m\]\w\[\033[00m\]\$ '
else
    PS1='${debian_chroot:+($debian_chroot)}\u@\h:\w\$ '
fi
unset color_prompt force_color_prompt

# If this is an xterm set the title to user@host:dir
case "$TERM" in
xterm*|rxvt*)
    PS1="\[\e]0;${debian_chroot:+($debian_chroot)}\u@\h: \w\a\]$PS1"
    ;;
*)
    ;;
esac

# enable color support of ls and also add handy aliases
if [ -x /usr/bin/dircolors ]; then
    test -r ~/.dircolors && eval "$(dircolors -b ~/.dircolors)" || eval "$(dircolors -b)"
    alias ls='ls --color=auto'
    #alias dir='dir --color=auto'
    #alias vdir='vdir --color=auto'

    alias grep='grep --color=auto'
    alias fgrep='fgrep --color=auto'
    alias egrep='egrep --color=auto'
fi

# colored GCC warnings and errors
#export GCC_COLORS='error=01;31:warning=01;35:note=01;36:caret=01;32:locus=01:quote=01'

# some more ls aliases
alias ll='ls -alF'
alias la='ls -A'
alias l='ls -CF'

# Add an "alert" alias for long running commands.  Use like so:
#   sleep 10; alert
alias alert='notify-send --urgency=low -i "$([ $? = 0 ] && echo terminal || echo error)" "$(history|tail -n1|sed -e '\''s/^\s*[0-9]\+\s*//;s/[;&|]\s*alert$//'\'')"'

# Alias definitions.
# You may want to put all your additions into a separate file like
# ~/.bash_aliases, instead of adding them here directly.
# See /usr/share/doc/bash-doc/examples in the bash-doc package.

if [ -f ~/.bash_aliases ]; then
    . ~/.bash_aliases
fi

# enable programmable completion features (you don't need to enable
# this, if it's already enabled in /etc/bash.bashrc and /etc/profile
# sources /etc/bash.bashrc).
if ! shopt -oq posix; then
  if [ -f /usr/share/bash-completion/bash_completion ]; then
    . /usr/share/bash-completion/bash_completion
  elif [ -f /etc/bash_completion ]; then
    . /etc/bash_completion
  fi
fi

# enter the course environment: micromamba images, else conda
if [ -f /usr/local/bin/_activate_current_env.sh ]; then
    . /usr/local/bin/_activate_current_env.sh
elif command -v conda >/dev/null 2>&1; then
    eval "$(conda shell.bash hook)"
fi
`

// buildVolumeSeedCommand returns the volume-init step that seeds an empty student
// volume with seedProfile and seedBashrc, and gives the volume root and both
// files to uid:gid. It does not rely on the root chown that follows it. It writes
// nothing into a volume that holds anything, and fails, so the guard reports it,
// when it cannot list the volume.
//
// The files travel base64-encoded so their quotes never meet the shell. Each is
// written complete to a temporary name, then renamed into place without
// overwriting (mv -n, whose exit code differs between busybox and GNU, so the
// step checks that both files are in place instead). A write that fails leaves
// the volume empty again, so Docker copies as before and the next start
// retries. A rename that fails after the other one succeeded leaves a file
// behind: the step is reported, and the volume is then neither seeded again nor
// copied into.
//
// Two starts of the same student can seed at the same moment. A volume that
// holds a seed's temporary files and nothing else but .profile and .bashrc is
// another seed in progress: it is waited on, up to seedWaitSeconds, listing it
// again each second. Temporary files still there after that are taken as
// abandoned (a volume-init killed mid-step), removed once, and the volume is
// judged again. Only files named seedTempPrefix, a dot and six characters are
// ever removed. Two seeds that both find the volume empty both write, and the
// rename without overwrite keeps the first file in place. A temporary file the
// step cannot remove fails the step, so the guard reports it: left in place it
// keeps the volume from being seeded or copied into.
func buildVolumeSeedCommand(uid, gid string) string {
	// One listing per turn decides: empty, seed; anything but the seed's own
	// names, or no temporary file, leave alone; else another seed is in
	// progress, wait, and once the wait is over remove its temporary files a
	// single time and decide again.
	return fmt.Sprintf(`{ n=0; cleared=; s=0; while :; do `+
		`entries=$(ls -A %[1]s) || { s=$?; break; }; `+
		`if [ -z "$entries" ]; then %[2]s; s=$?; break; fi; `+
		`temps=$(printf '%%s\n' "$entries" | grep -c '^\%[3]s\.......$'); `+
		`other=$(printf '%%s\n' "$entries" | grep -v -e '^\%[3]s\.......$' -e '^\.profile$' -e '^\.bashrc$'); `+
		`if [ "$temps" -eq 0 ] || [ -n "$other" ] || [ -n "$cleared" ]; then break; fi; `+
		`if [ $n -ge %[4]d ]; then rm -f %[1]s/%[3]s.?????? || { s=$?; break; }; cleared=1; continue; fi; `+
		`sleep 1; n=$((n+1)); done; [ $s -eq 0 ]; }`,
		MOUNT_TARGET, buildVolumeSeedWrite(uid, gid), seedTempPrefix, seedWaitSeconds)
}

// seedTempPrefix names the seed's temporary files: the prefix, a dot and the six
// characters mktemp puts in place of XXXXXX. The grep pattern (six dots) and the
// rm glob (six ?) in buildVolumeSeedCommand match exactly that; change all three
// together. Only files named so are ever removed as abandoned.
const seedTempPrefix = ".mydocker-seed"

// seedWaitSeconds caps how long a seed waits for another seed of the same volume
// to finish. A seed takes well under a second; the margin is for a stalled NFS
// server, so that a live seed is not taken for an abandoned one. Elapsed time
// cannot prove a seed dead: a seed stalled longer than this can still be
// displaced, and its student then starts once without the seed. Temporary
// files left by a volume-init killed mid-step cost one wait of this length.
var seedWaitSeconds = 60

// buildVolumeSeedWrite returns the part of the seed that writes the files into
// an empty volume.
func buildVolumeSeedWrite(uid, gid string) string {
	encode := func(content string) string {
		return base64.StdEncoding.EncodeToString([]byte(content))
	}
	return fmt.Sprintf(`{ p=; b=; `+
		`p=$(mktemp %[1]s/%[6]s.XXXXXX) && b=$(mktemp %[1]s/%[6]s.XXXXXX) && `+
		`printf '%%s\n' %[2]s | base64 -d > "$p" && printf '%%s\n' %[3]s | base64 -d > "$b" && `+
		`chmod 644 "$p" "$b" && chown %[4]s:%[5]s "$p" "$b" %[1]s && `+
		`{ mv -n "$p" %[1]s/.profile; mv -n "$b" %[1]s/.bashrc; [ -e %[1]s/.profile ] && [ -e %[1]s/.bashrc ]; }; `+
		`rc=$?; rm -f ${p:+"$p"} ${b:+"$b"} || { r=$?; [ $rc -ne 0 ] || rc=$r; }; [ $rc -eq 0 ]; }`,
		MOUNT_TARGET, encode(seedProfile), encode(seedBashrc), uid, gid, seedTempPrefix)
}
