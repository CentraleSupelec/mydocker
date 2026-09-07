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
//	{{PORT['<digits>']}}    or    {{PORT["<digits>"]}}
//
// The opening and closing quotes must match, and <digits> is the container port the
// course declares as a port to map. It is replaced by the host port that port was
// mapped to. A placeholder that survives substitution reaches the container as a
// literal, so every unresolved case is logged.
const commandPortPrefix = "{{PORT["

// How much of a malformed candidate to quote in the log line.
const candidateExcerptLength = 32

var commandPortPattern = regexp.MustCompile(`\{\{PORT\[(?:'(\d+)'|"(\d+)")\]\}\}`)

func substituteCommandPorts(command string, ports []*pb.ResponsePort) string {
	logMalformedCommandPortCandidates(command)

	return commandPortPattern.ReplaceAllStringFunc(command, func(placeholder string) string {
		submatches := commandPortPattern.FindStringSubmatch(placeholder)
		if submatches == nil {
			return placeholder
		}

		digits := submatches[1]
		if digits == "" {
			digits = submatches[2]
		}

		portToMap, err := strconv.Atoi(digits)
		if err != nil {
			log.Warnf("Could not read the port in command placeholder %s, leaving it as is", placeholder)
			return placeholder
		}

		for _, port := range ports {
			if port.PortToMap == uint32(portToMap) {
				return fmt.Sprintf("%d", port.MapTo)
			}
		}

		log.Warnf("Command placeholder %s asks for a port the container does not map, leaving it as is", placeholder)
		return placeholder
	})
}

// logMalformedCommandPortCandidates reports every occurrence of the opening {{PORT[ that
// does not begin a canonical placeholder at that exact offset. The validators reject these
// before a course can be saved, so reaching here means a command predates the validators or
// bypassed them; the text survives into the container either way, and this is the only trace
// of it.
func logMalformedCommandPortCandidates(command string) {
	for index := strings.Index(command, commandPortPrefix); index >= 0; {
		match := commandPortPattern.FindStringIndex(command[index:])
		if match == nil || match[0] != 0 {
			end := index + candidateExcerptLength
			if end > len(command) {
				end = len(command)
			}
			log.Warnf(
				"Malformed port placeholder %q in command, leaving it as is",
				command[index:end],
			)
		}

		next := strings.Index(command[index+len(commandPortPrefix):], commandPortPrefix)
		if next < 0 {
			return
		}
		index += len(commandPortPrefix) + next
	}
}
