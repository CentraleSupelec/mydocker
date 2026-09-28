package main

import (
	"unicode/utf8"

	pb "github.com/centralesupelec/mydocker/docker-api/protobuf"
	"github.com/docker/docker/pkg/namesgenerator"
	log "github.com/sirupsen/logrus"
)

const (
	// containerPasswordLength is what we generate. It sits above the minimum so the limit can rise
	// a little without another release.
	containerPasswordLength = 16

	// minimumContainerPasswordLength is what consumers inside the image demand. filebrowser
	// refuses anything shorter than 12 characters and exits non-zero, which takes the whole
	// environment down in a restart loop rather than degrading one feature.
	minimumContainerPasswordLength = 12
)

// isUsableContainerPassword reports whether a password can be handed to an image as it is.
// Counted in characters rather than bytes, because the limit the image enforces is a character
// count.
func isUsableContainerPassword(password string) bool {
	return utf8.RuneCountInString(password) >= minimumContainerPasswordLength
}

// resolveContainerPassword picks the credential to hand to the image, given whatever the request
// carried.
//
// The back end persists the credential from our response and replays it on every later request for
// the same user and course, so a password generated before the minimum existed comes back to us
// again and again. Accepting it would keep that environment broken permanently, and no amount of
// retrying by the student can help. Replacing it here repairs the account on its next start, and
// the response carries the new password back for the back end to store.
//
// The username is kept when there is one: it is what the student uses to connect, it is not the
// part that fails validation, and changing it needlessly would invalidate instructions they
// already have.
func resolveContainerPassword(stored *pb.UserPasswordMethod) *pb.UserPasswordMethod {
	fresh := &pb.UserPasswordMethod{
		Username: namesgenerator.GetRandomName(0),
		Password: randPassword(containerPasswordLength),
	}

	if stored == nil {
		return fresh
	}

	if isUsableContainerPassword(stored.GetPassword()) {
		return stored
	}

	if stored.GetUsername() != "" {
		fresh.Username = stored.GetUsername()
	}
	log.Warnf(
		"replacing stored password for user %s: shorter than the %d characters the image requires",
		fresh.Username, minimumContainerPasswordLength,
	)

	return fresh
}

// containerMustBeReplacedForPassword reports whether a service that is already running has to be
// recreated because the credential it was built with can no longer start the image.
//
// Two conditions, and the second one bounds the blast radius. Every service created before the
// minimum existed carries a short password, so a rule that looked only at length would delete and
// recreate every environment on the platform at its next request, including the ones that are
// serving a student right now. A running environment is evidence that its image accepts its
// credential, whatever the length, so only a service that is not running is a candidate. The rest
// repair themselves the next time they are started.
//
// A service with no password is left alone, and that case has to be recognised by an empty password
// rather than by a missing credential. exist() builds the wrapper and the message unconditionally
// and fills them from the service's environment variables, so a service that authenticates some
// other way still arrives here as a credential whose fields are empty strings. Reading that as an
// unusably short password would recreate every such service on every single request, for ever.
func containerMustBeReplacedForPassword(
	fromService *pb.ContainerResponse_UserPassword,
	serviceIsRunning bool,
) bool {
	if serviceIsRunning {
		return false
	}

	if fromService == nil || fromService.UserPassword == nil {
		return false
	}

	password := fromService.UserPassword.GetPassword()
	if password == "" {
		return false
	}

	return !isUsableContainerPassword(password)
}

// storedContainerPassword picks which existing credential to offer to resolveContainerPassword.
//
// It can arrive from two places: the request, carrying what the back end persisted, and the service
// being replaced, carrying what was actually handed to the image. They normally agree. The request
// wins when both are present, because it is the side that will persist whatever comes back, and the
// service is the fallback so that replacing one does not needlessly change the username the student
// connects with.
func storedContainerPassword(
	fromRequest *pb.UserPasswordMethod,
	fromService *pb.ContainerResponse_UserPassword,
) *pb.UserPasswordMethod {
	if fromRequest != nil {
		return fromRequest
	}
	if fromService == nil {
		return nil
	}
	return fromService.UserPassword
}
