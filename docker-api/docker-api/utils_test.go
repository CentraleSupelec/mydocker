package main

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestRandPassword(t *testing.T) {
	p1 := randPassword(10)
	t.Log(p1)
	assert.Equal(t, len(p1), 10)
	p2 := randPassword(10)
	assert.NotEqual(t, p1, p2)
}

func validateCommand(t *testing.T, actualParts []string, expectedParts []string) {
	assert.Equal(t, len(actualParts), len(expectedParts))

	for index, expectedPart := range expectedParts {
		assert.Equal(t, expectedPart, actualParts[index])
	}
}
func TestCommandToParts(t *testing.T) {
	var command string
	var parts []string
	var expected []string

	// Typical command
	command = `start-notebook.sh --NotebookApp.token={{PASSWORD}}`
	parts = commandToParts(command)
	expected = []string{"start-notebook.sh", `--NotebookApp.token={{PASSWORD}}`}
	validateCommand(t, parts, expected)

	// Example with default command "{{USERNAME}} {{PASSWORD}}" after replacement
	command = `friendly_cat xTz31oi3e`
	parts = commandToParts(command)
	expected = []string{"friendly_cat", "xTz31oi3e"}
	validateCommand(t, parts, expected)

	// Word's last part is param expression
	command = `./mycommand -username={{USERNAME}}`
	parts = commandToParts(command)
	expected = []string{"./mycommand", "-username={{USERNAME}}"}
	validateCommand(t, parts, expected)

	// Word's last part is param expression simplified
	command = `./mycommand -username={{USERNAME}}`
	parts = commandToParts(command)
	expected = []string{"./mycommand", "-username={{USERNAME}}"}
	validateCommand(t, parts, expected)

	// Word's first part is single quote without dollar
	command = `mkdir 'TERM'-test`
	parts = commandToParts(command)
	expected = []string{"mkdir", "'TERM'-test"}
	validateCommand(t, parts, expected)

	// Word's last part is single quote without dollar
	command = `mkdir test-'TERM'`
	parts = commandToParts(command)
	expected = []string{"mkdir", "test-'TERM'"}
	validateCommand(t, parts, expected)

	// Word's first part is single quote with dollar
	command = `mkdir $'TERM'-test`
	parts = commandToParts(command)
	expected = []string{"mkdir", "$'TERM'-test"}
	validateCommand(t, parts, expected)

	// Word's last part is single quote with dollar
	command = `mkdir test-$'TERM'`
	parts = commandToParts(command)
	expected = []string{"mkdir", "test-$'TERM'"}
	validateCommand(t, parts, expected)

	// Word's first part is command substitution
	command = "c $(python ./test.py)=test"
	parts = commandToParts(command)
	expected = []string{"c", `$(python ./test.py)=test`}
	validateCommand(t, parts, expected)

	// Word's last part is command substitution
	command = "c -date=$(python ./test.py)"
	parts = commandToParts(command)
	expected = []string{"c", `-date=$(python ./test.py)`}
	validateCommand(t, parts, expected)

	// Word is double quote (start and finish)
	command = `b "s -v=${T}"`
	parts = commandToParts(command)
	expected = []string{"b", "s -v=${T}"}
	validateCommand(t, parts, expected)

	// Word's first part is double quote with dollar
	command = `mkdir $"TERM"-test`
	parts = commandToParts(command)
	expected = []string{"mkdir", `$"TERM"-test`}
	validateCommand(t, parts, expected)

	// Word's last part is double quote with dollar
	command = `mkdir test-$"TERM"`
	parts = commandToParts(command)
	expected = []string{"mkdir", `test-$"TERM"`}
	validateCommand(t, parts, expected)

	// Word's last part is double quote without dollar
	command = `c -v="h"`
	parts = commandToParts(command)
	expected = []string{"c", `-v="h"`}
	validateCommand(t, parts, expected)
}
