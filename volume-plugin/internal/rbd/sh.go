package dockerVolumeRbd

import (
	"bytes"
	"context"
	"errors"
	"os"
	"os/exec"
	"strconv"
	"strings"
	"syscall"
	"time"
)

var (
	defaultShellTimeout = shellTimeoutFromEnv()
)

// shellTimeoutFromEnv reads SHELL_TIMEOUT_SECONDS (plugin settable env).
// Default 90s: must stay under dockerd's 120s plugin proxy deadline, or our
// response lands in an already-cancelled context on the daemon side.
func shellTimeoutFromEnv() time.Duration {
	if v := os.Getenv("SHELL_TIMEOUT_SECONDS"); v != "" {
		if n, err := strconv.Atoi(v); err == nil && n > 0 {
			return time.Duration(n) * time.Second
		}
	}
	return 90 * time.Second
}

// shWithTimeout runs the command and waits at most howLong. On timeout the
// whole process group is killed: a timed-out command must not keep running
// unobserved, and rbd can fork helpers that a plain Process.Kill would orphan.
func shWithTimeout(howLong time.Duration, name string, args ...string) (string, error) {
	ctx, cancel := context.WithTimeout(context.Background(), howLong)
	defer cancel()

	cmd := exec.CommandContext(ctx, name, args...)
	var stdout bytes.Buffer
	cmd.Stdout = &stdout
	// child stderr goes to the plugin's stderr (dockerd plugin logs); the
	// rbd shim's RBD_SHIM lines and ceph CLI errors must stay visible
	cmd.Stderr = os.Stderr
	cmd.SysProcAttr = &syscall.SysProcAttr{Setpgid: true}
	cmd.Cancel = func() error {
		return syscall.Kill(-cmd.Process.Pid, syscall.SIGKILL)
	}
	cmd.WaitDelay = 10 * time.Second

	err := cmd.Run()
	if ctx.Err() == context.DeadlineExceeded {
		return "", errors.New("timeout reached")
	}
	return strings.Trim(stdout.String(), " \n"), err
}

// shWithDefaultTimeout will use the defaultShellTimeout so you dont have to pass one
func shWithDefaultTimeout(name string, args ...string) (string, error) {
	return shWithTimeout(defaultShellTimeout, name, args...)
}
