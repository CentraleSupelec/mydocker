package main

import (
	"fmt"
	"regexp"
	"strconv"

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
var commandPortPattern = regexp.MustCompile(`\{\{PORT\[(?:'(\d+)'|"(\d+)")\]\}\}`)

func substituteCommandPorts(command string, ports []*pb.ResponsePort) string {
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
