package main

import (
	"testing"

	pb "github.com/centralesupelec/mydocker/docker-api/protobuf"
	"github.com/stretchr/testify/assert"
)

// Conformance cases for the command port placeholder grammar. The same table is
// implemented against the back-end and front-end validators; a change here belongs in
// all three at once. "unchanged" means the placeholder reaches the container as a
// literal, which the validators exist to prevent.
func TestSubstituteCommandPorts(t *testing.T) {
	ports := []*pb.ResponsePort{
		{PortToMap: 8080, MapTo: 10123},
		{PortToMap: 22, MapTo: 10456},
	}

	tests := []struct {
		name     string
		command  string
		expected string
	}{
		{"single quotes", "serve --port {{PORT['8080']}}", "serve --port 10123"},
		{"double quotes", `serve --port {{PORT["8080"]}}`, "serve --port 10123"},
		{"several placeholders", "a {{PORT['8080']}} b {{PORT['22']}}", "a 10123 b 10456"},
		{"same placeholder twice", "{{PORT['22']}} {{PORT['22']}}", "10456 10456"},
		{"no placeholder", "sleep infinity", "sleep infinity"},
		{"empty command", "", ""},
		{"unmapped port stays literal", "serve {{PORT['9999']}}", "serve {{PORT['9999']}}"},
		{"unquoted is not a placeholder", "serve {{PORT[8080]}}", "serve {{PORT[8080]}}"},
		{"mismatched quotes are not a placeholder", `serve {{PORT['8080"]}}`, `serve {{PORT['8080"]}}`},
		{"non numeric is not a placeholder", "serve {{PORT['http']}}", "serve {{PORT['http']}}"},
		{"other placeholders are untouched", "{{USERNAME}} {{PORT['22']}}", "{{USERNAME}} 10456"},
		{"unterminated candidate stays literal", "serve {{PORT[8080", "serve {{PORT[8080"},
		{"candidate missing one closing brace stays literal", "serve {{PORT['8080']}", "serve {{PORT['8080']}"},
		{"bare opening candidate stays literal", "serve {{PORT[", "serve {{PORT["},
		{"malformed after a valid one", "{{PORT['22']}} {{PORT[8080", "10456 {{PORT[8080"},
	}

	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			assert.Equal(t, test.expected, substituteCommandPorts(test.command, ports))
		})
	}
}

// The validators reject malformed candidates before a course can be saved, so these cases
// only assert that the substitution leaves them alone rather than mangling the command. The
// warning they log is the only trace they leave.
func TestSubstituteCommandPortsWithoutPorts(t *testing.T) {
	assert.Equal(t, "serve {{PORT['8080']}}", substituteCommandPorts("serve {{PORT['8080']}}", nil))
}
