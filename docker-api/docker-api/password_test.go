package main

import (
	"testing"

	pb "github.com/centralesupelec/mydocker/docker-api/protobuf"
)

// What we generate has to satisfy what the image enforces. These two constants are set in
// different places for different reasons, and a release that lowered the generated length below
// the minimum would crash-loop every new environment.
func TestGeneratedPasswordSatisfiesTheMinimum(t *testing.T) {
	if containerPasswordLength < minimumContainerPasswordLength {
		t.Fatalf(
			"generated length %d is below the minimum %d",
			containerPasswordLength, minimumContainerPasswordLength,
		)
	}
	if password := randPassword(containerPasswordLength); !isUsableContainerPassword(password) {
		t.Errorf("generated password %q is not usable", password)
	}
}

func TestPasswordUsabilityBoundary(t *testing.T) {
	cases := []struct {
		name     string
		password string
		usable   bool
	}{
		{"one short of the minimum", "abcdefghijk", false},
		{"exactly the minimum", "abcdefghijkl", true},
		{"one over the minimum", "abcdefghijklm", true},
		{"empty", "", false},
		{"the length we used to generate", "Ag0tXJFvsG", false},
	}
	for _, c := range cases {
		t.Run(c.name, func(t *testing.T) {
			if got := isUsableContainerPassword(c.password); got != c.usable {
				t.Errorf("isUsableContainerPassword(%q) = %v, want %v", c.password, got, c.usable)
			}
		})
	}
}

func TestResolveContainerPasswordGeneratesWhenNothingStored(t *testing.T) {
	resolved := resolveContainerPassword(nil)

	if resolved == nil {
		t.Fatal("no credential returned")
	}
	if resolved.GetUsername() == "" {
		t.Error("username is empty")
	}
	if !isUsableContainerPassword(resolved.GetPassword()) {
		t.Errorf("generated password %q is not usable", resolved.GetPassword())
	}
}

func TestResolveContainerPasswordKeepsAUsableStoredCredential(t *testing.T) {
	stored := &pb.UserPasswordMethod{Username: "quirky_hopper", Password: "abcdefghijklmnop"}

	resolved := resolveContainerPassword(stored)

	if resolved.GetUsername() != stored.GetUsername() {
		t.Errorf("username = %q, want %q", resolved.GetUsername(), stored.GetUsername())
	}
	if resolved.GetPassword() != stored.GetPassword() {
		t.Error("a usable stored password was replaced")
	}
}

// The regression this whole change exists for: the back end replays a credential issued before the
// minimum existed, so it arrives on every request and has to be repaired rather than passed on.
// The username survives, because the student connects with it and it is not what fails validation.
func TestResolveContainerPasswordRepairsALegacyShortCredential(t *testing.T) {
	stored := &pb.UserPasswordMethod{Username: "lucid_jang", Password: "Ag0tXJFvsG"}

	resolved := resolveContainerPassword(stored)

	if resolved.GetUsername() != "lucid_jang" {
		t.Errorf("username = %q, want it kept as lucid_jang", resolved.GetUsername())
	}
	if resolved.GetPassword() == stored.GetPassword() {
		t.Error("the legacy short password was handed to the image unchanged")
	}
	if !isUsableContainerPassword(resolved.GetPassword()) {
		t.Errorf("replacement password %q is not usable either", resolved.GetPassword())
	}
}

func TestResolveContainerPasswordGeneratesAUsernameWhenStoredHasNone(t *testing.T) {
	stored := &pb.UserPasswordMethod{Password: "short"}

	resolved := resolveContainerPassword(stored)

	if resolved.GetUsername() == "" {
		t.Error("username is empty, nothing would be able to log in")
	}
	if !isUsableContainerPassword(resolved.GetPassword()) {
		t.Errorf("password %q is not usable", resolved.GetPassword())
	}
}

// Two regressions live in this one predicate. A service that already exists short-circuits the
// create path and returns its own credential, so an unusable one has to force a replacement.
// And every service created before the minimum existed carries a short password, so replacing on
// length alone would recreate every environment on the platform, including the ones serving a
// student. Only a service that is not running is a candidate.
func TestContainerMustBeReplacedForPassword(t *testing.T) {
	legacy := &pb.ContainerResponse_UserPassword{UserPassword: &pb.UserPasswordMethod{Username: "lucid_jang", Password: "Ag0tXJFvsG"}}

	cases := []struct {
		name             string
		fromService      *pb.ContainerResponse_UserPassword
		serviceIsRunning bool
		replace          bool
	}{
		{
			name:        "stopped service with a legacy ten character credential",
			fromService: legacy,
			replace:     true,
		},
		{
			// The blast radius case: this is every environment on the platform after an upgrade.
			name:             "running service with a legacy ten character credential",
			fromService:      legacy,
			serviceIsRunning: true,
			replace:          false,
		},
		{
			name:        "stopped service with a credential at the minimum",
			fromService: &pb.ContainerResponse_UserPassword{UserPassword: &pb.UserPasswordMethod{Username: "lucid_jang", Password: "abcdefghijkl"}},
			replace:     false,
		},
		{
			name:        "no credential at all",
			fromService: nil,
			replace:     false,
		},
		{
			name:        "wrapper with no message at all",
			fromService: &pb.ContainerResponse_UserPassword{},
			replace:     false,
		},
		{
			// The shape exist() actually returns for a service that does not authenticate by
			// password: the wrapper and the message are always built, and the fields come back as
			// empty strings.
			name:        "production shape of a service with no password",
			fromService: &pb.ContainerResponse_UserPassword{UserPassword: &pb.UserPasswordMethod{}},
			replace:     false,
		},
		{
			name:        "production shape with a username but no password",
			fromService: &pb.ContainerResponse_UserPassword{UserPassword: &pb.UserPasswordMethod{Username: "lucid_jang"}},
			replace:     false,
		},
	}
	for _, c := range cases {
		t.Run(c.name, func(t *testing.T) {
			if got := containerMustBeReplacedForPassword(c.fromService, c.serviceIsRunning); got != c.replace {
				t.Errorf("containerMustBeReplacedForPassword() = %v, want %v", got, c.replace)
			}
		})
	}
}

func TestStoredContainerPasswordPrefersTheRequest(t *testing.T) {
	fromRequest := &pb.UserPasswordMethod{Username: "from_request", Password: "Ag0tXJFvsG"}
	fromService := &pb.ContainerResponse_UserPassword{UserPassword: &pb.UserPasswordMethod{Username: "from_service", Password: "Ag0tXJFvsG"}}

	if got := storedContainerPassword(fromRequest, fromService); got.GetUsername() != "from_request" {
		t.Errorf("username = %q, want from_request", got.GetUsername())
	}
}

// Replacing a service must not silently change the username the student connects with, even when
// the back end sent nothing to reuse.
func TestStoredContainerPasswordFallsBackToTheServiceBeingReplaced(t *testing.T) {
	fromService := &pb.ContainerResponse_UserPassword{UserPassword: &pb.UserPasswordMethod{Username: "lucid_jang", Password: "Ag0tXJFvsG"}}

	stored := storedContainerPassword(nil, fromService)
	if stored.GetUsername() != "lucid_jang" {
		t.Fatalf("username = %q, want lucid_jang", stored.GetUsername())
	}

	resolved := resolveContainerPassword(stored)
	if resolved.GetUsername() != "lucid_jang" {
		t.Errorf("resolved username = %q, want lucid_jang", resolved.GetUsername())
	}
	if !isUsableContainerPassword(resolved.GetPassword()) {
		t.Errorf("resolved password %q is not usable", resolved.GetPassword())
	}
}

func TestStoredContainerPasswordHandlesNothingStoredAnywhere(t *testing.T) {
	if stored := storedContainerPassword(nil, nil); stored != nil {
		t.Errorf("storedContainerPassword(nil, nil) = %v, want nil", stored)
	}
}
