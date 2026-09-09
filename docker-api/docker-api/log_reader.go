package main

import (
	"bytes"
	"io"

	"github.com/docker/docker/pkg/stdcopy"
)

// readTaskLogs decodes a Docker log stream and returns at most tailLines lines,
// keeping the most recent ones. A tailLines of zero or less means no limit.
//
// It replaces readLogs on every path that reads a stream which ends on its own.
// readLogs stays for the image-build path, which reads a followed stream in a
// poll loop and relies on its read timeout to return at all. Three reasons to
// prefer this one where the stream terminates:
//
//   - readLogs strips eight bytes from every line and drops lines shorter than
//     that, because it treats the stream's frame header as a per-line prefix.
//     stdcopy reads the framing properly, so short lines survive.
//   - readLogs stops when no line arrives within LogsTimeout, so a busy
//     container is cut wherever a gap in its output happens to fall.
//   - readLogs leaks a goroutine for every read that hits that timeout.
//
// stdout and stderr are written to the same sink on purpose: the caller wants
// the output in the order the container produced it, not two separate streams.
func readTaskLogs(reader io.Reader, tailLines int) (string, bool, error) {
	sink := newTailWriter(tailLines)
	if _, err := stdcopy.StdCopy(sink, sink, reader); err != nil {
		return "", false, err
	}
	return sink.String(), sink.truncated, nil
}

// tailWriter keeps the last max lines written to it, so memory stays bounded by
// the limit rather than by how much the container printed.
type tailWriter struct {
	max       int
	lines     [][]byte
	partial   []byte
	truncated bool
}

func newTailWriter(max int) *tailWriter {
	return &tailWriter{max: max}
}

func (w *tailWriter) Write(p []byte) (int, error) {
	written := len(p)
	buffer := append(w.partial, p...)

	for {
		index := bytes.IndexByte(buffer, '\n')
		if index < 0 {
			break
		}
		w.appendLine(buffer[:index])
		buffer = buffer[index+1:]
	}

	w.partial = append([]byte(nil), buffer...)
	return written, nil
}

func (w *tailWriter) appendLine(line []byte) {
	w.lines = append(w.lines, append([]byte(nil), line...))
	if w.max > 0 && len(w.lines) > w.max {
		w.lines = w.lines[len(w.lines)-w.max:]
		w.truncated = true
	}
}

func (w *tailWriter) String() string {
	lines := w.lines
	if len(w.partial) > 0 {
		lines = append(lines, w.partial)
	}
	if len(lines) == 0 {
		return ""
	}
	return string(bytes.Join(lines, []byte("\n")))
}
