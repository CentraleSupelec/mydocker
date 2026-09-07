package main

import (
	"fmt"
	"regexp"
	"strconv"
	"strings"

	pb "github.com/centralesupelec/mydocker/docker-api/protobuf"
	log "github.com/sirupsen/logrus"
)

// Canonical grammar for the port placeholder a course may use in its launch command.
// The front-end and back-end validators implement the same grammar and reject anything
// else before it can reach here, so the three must be changed together.
//
//	{{PORT['<port>']}}    or    {{PORT["<port>"]}}
//
// The opening and closing quotes must match, and <port> is the container port the course
// declares as a port to map: no leading zero, and no larger than 65535. It is replaced by
// the host port that port was mapped to.
//
// Leading zeros are refused rather than normalised. This side would resolve "08080"
// numerically to 8080 while the validators compare it as text against the declared ports
// and see an unknown one, so the three would disagree about the same command.
//
// A placeholder that survives substitution reaches the container as a literal, so every
// unresolved case is logged.
const commandPortPrefix = "{{PORT["

const commandPortSuffix = "}}"

// How much of a malformed candidate to quote in the log line.
const candidateExcerptLength = 32

var commandPortPattern = regexp.MustCompile(`\{\{PORT\[(?:'([1-9][0-9]*)'|"([1-9][0-9]*)")\]\}\}`)

// substituteCommandPorts walks the command once, left to right, replacing canonical
// placeholders and copying everything else through untouched.
//
// The single walk is what keeps a malformed candidate inert. Replacing by pattern instead
// would rewrite canonical-looking text nested inside a malformed candidate:
// "{{PORT[{{PORT['8080']}}" would become "{{PORT[10123", mutating a command this function
// had just reported it was leaving alone.
func substituteCommandPorts(command string, ports []*pb.ResponsePort) string {
	var out strings.Builder
	position := 0

	for {
		offset := strings.Index(command[position:], commandPortPrefix)
		if offset < 0 {
			out.WriteString(command[position:])
			return out.String()
		}
		candidate := position + offset
		out.WriteString(command[position:candidate])

		if match := commandPortPattern.FindStringSubmatchIndex(command[candidate:]); match != nil && match[0] == 0 {
			end := candidate + match[1]
			out.WriteString(resolveCommandPort(command[candidate:end], command, match, candidate, ports))
			position = end
			continue
		}

		end := endOfCandidate(command, candidate)
		log.Warnf(
			"Malformed port placeholder %q in command, leaving it as is",
			excerpt(command, candidate),
		)
		out.WriteString(command[candidate:end])
		position = end
	}
}

// resolveCommandPort returns the mapped host port for a canonical placeholder, or the
// placeholder unchanged when it cannot be resolved. The validators reject everything it
// logs about, so reaching those branches means a command predates the validators or
// bypassed them, and this is the only trace it leaves.
func resolveCommandPort(
	placeholder string,
	command string,
	match []int,
	candidate int,
	ports []*pb.ResponsePort,
) string {
	digits := ""
	if match[2] >= 0 {
		digits = command[candidate+match[2] : candidate+match[3]]
	} else if match[4] >= 0 {
		digits = command[candidate+match[4] : candidate+match[5]]
	}

	// ParseUint with an explicit bit size, not Atoi: Atoi accepts anything an int holds, so
	// on a 64-bit build "4294975376" parsed cleanly and then wrapped to 8080 on conversion,
	// substituting a port the course never asked for.
	portToMap, err := strconv.ParseUint(digits, 10, 16)
	if err != nil {
		log.Warnf("Port in command placeholder %s is out of range, leaving it as is", placeholder)
		return placeholder
	}

	for _, port := range ports {
		if port.PortToMap == uint32(portToMap) {
			return fmt.Sprintf("%d", port.MapTo)
		}
	}

	log.Warnf("Command placeholder %s asks for a port the container does not map, leaving it as is", placeholder)
	return placeholder
}

// endOfCandidate returns the index just past a malformed candidate, so the walk can copy it
// through in one piece. A candidate ends at the first closing "}}" after its opening, and an
// unterminated one runs to the end of the command.
func endOfCandidate(command string, candidate int) int {
	body := candidate + len(commandPortPrefix)
	if body > len(command) {
		return len(command)
	}
	if offset := strings.Index(command[body:], commandPortSuffix); offset >= 0 {
		return body + offset + len(commandPortSuffix)
	}
	return len(command)
}

func excerpt(command string, candidate int) string {
	end := candidate + candidateExcerptLength
	if end > len(command) {
		end = len(command)
	}
	return command[candidate:end]
}
