package main

import (
	"fmt"
	"os"
	"os/exec"
	"path/filepath"
	"strings"
	"time"

	pb "github.com/centralesupelec/mydocker/docker-api/protobuf"
)

// withSeed sets StudentVolumeSeed for one test and returns the restore.
func withSeed(on bool) func() {
	saved := c.StudentVolumeSeed
	c.StudentVolumeSeed = on
	return func() { c.StudentVolumeSeed = saved }
}

// selfRunUid is a run uid the test process may chown to without root.
func selfRunUid() string {
	return fmt.Sprintf("%d:%d", os.Getuid(), os.Getgid())
}

func (suite *ContainerTestSuite) TestVolumeInitSeedOnlyWithRunUidAndSwitch() {
	defer withSeed(true)()
	paths := []*pb.Path{{Value: "*/.cache", Recursive: true, Permissions: 777, ChangeOwner: true}}

	suite.Contains(buildVolumeInitCommand(nil, "", "1000:100"), "ls -A /student_volume", "run uid, switch on")
	suite.NotContains(buildVolumeInitCommand(paths, "57439", ""), "ls -A", "no run uid")

	c.StudentVolumeSeed = false
	suite.NotContains(buildVolumeInitCommand(nil, "", "1000:100"), "ls -A", "switch off")
}

func (suite *ContainerTestSuite) TestVolumeInitSeedRunsFirstAndOwnsItsFiles() {
	defer withSeed(true)()

	cmd := buildVolumeInitCommand(nil, "", "1000:100")

	suite.True(strings.HasPrefix(cmd, "{ { n=0; cleared=; s=0; while :; do entries=$(ls -A /student_volume)"), "the seed is the first step")
	suite.Less(strings.Index(cmd, "ls -A"), strings.Index(cmd, "-maxdepth 1"), "seed before the root chown")
	suite.Contains(cmd, `chown 1000:100 "$p" "$b" /student_volume &&`)
	suite.Contains(cmd, volumeInitRefusedMarker+"seed exit")

	suite.Contains(buildVolumeInitCommand(nil, "", "1000"), `chown 1000:1000 "$p" "$b" /student_volume `, "gid defaults to the uid")
}

// runVolumeInitWithStub runs a volume-init command on dir with one tool replaced
// by a stub script.
func runVolumeInitWithStub(suite *ContainerTestSuite, cmd, dir, tool, script string) (string, error) {
	stubs := suite.T().TempDir()
	suite.Require().NoError(os.WriteFile(filepath.Join(stubs, tool), []byte(script), 0o755))
	run := exec.Command("sh", "-c", strings.ReplaceAll(cmd, MOUNT_TARGET, dir))
	run.Env = append(os.Environ(), "PATH="+stubs+":"+os.Getenv("PATH"))
	out, err := run.CombinedOutput()
	return string(out), err
}

func entryNames(suite *ContainerTestSuite, dir string) []string {
	entries, err := os.ReadDir(dir)
	suite.Require().NoError(err)
	names := []string{}
	for _, e := range entries {
		names = append(names, e.Name())
	}
	return names
}

func (suite *ContainerTestSuite) TestVolumeInitSeedWritesBothFilesIntoAnEmptyVolume() {
	defer withSeed(true)()
	dir := suite.T().TempDir()

	out, err := runVolumeInit(suite, buildVolumeInitCommand(nil, "", selfRunUid()), dir)

	suite.NoError(err, out)
	suite.NotContains(out, volumeInitRefusedMarker)
	suite.Contains(out, volumeInitWalkMarker, "the walk still runs")
	suite.ElementsMatch([]string{".profile", ".bashrc"}, entryNames(suite, dir), "no temporary file left")
	for file, want := range map[string]string{".profile": seedProfile, ".bashrc": seedBashrc} {
		got, fileErr := os.ReadFile(filepath.Join(dir, file))
		suite.Require().NoError(fileErr, file)
		suite.Equal(want, string(got), "%s is written byte for byte", file)
		info, statErr := os.Stat(filepath.Join(dir, file))
		suite.Require().NoError(statErr, file)
		suite.Equal(os.FileMode(0o644), info.Mode().Perm(), file)
	}
}

func (suite *ContainerTestSuite) TestVolumeInitSeedLeavesANonEmptyVolumeAlone() {
	defer withSeed(true)()
	dir := suite.T().TempDir()
	suite.Require().NoError(os.WriteFile(filepath.Join(dir, ".bash_history"), []byte("ls\n"), 0o600))

	out, err := runVolumeInit(suite, buildVolumeInitCommand(nil, "", selfRunUid()), dir)

	suite.NoError(err, out)
	suite.NotContains(out, volumeInitRefusedMarker)
	suite.Equal([]string{".bash_history"}, entryNames(suite, dir), "a volume holding only a dotfile is not seeded")
}

func (suite *ContainerTestSuite) TestVolumeInitFailedSeedLeavesTheVolumeEmpty() {
	defer withSeed(true)()
	dir := suite.T().TempDir()

	out, err := runVolumeInit(suite, buildVolumeInitCommand(nil, "", selfRunUid()), dir, "base64")

	suite.NoError(err, out)
	suite.Contains(out, volumeInitRefusedMarker+"seed exit 1: base64: refused by test stub")
	suite.Contains(out, volumeInitWalkMarker, "the walk still runs")
	suite.Empty(entryNames(suite, dir), "the next start must find the volume empty and seed it")
}

func (suite *ContainerTestSuite) TestVolumeInitSeedReportsAVolumeItCannotList() {
	defer withSeed(true)()
	dir := suite.T().TempDir()

	out, err := runVolumeInit(suite, buildVolumeInitCommand(nil, "", selfRunUid()), dir, "ls")

	suite.NoError(err, out)
	suite.Contains(out, volumeInitRefusedMarker+"seed exit 1: ls: refused by test stub")
	suite.Empty(entryNames(suite, dir))
}

// Two starts of the same student seed at once: both saw the volume empty, the
// other one has already put its .profile in place, perhaps edited since. The
// rename must not overwrite it.
func (suite *ContainerTestSuite) TestVolumeInitSeedNeverOverwritesAFileInPlace() {
	defer withSeed(true)()
	dir := suite.T().TempDir()
	suite.Require().NoError(os.WriteFile(filepath.Join(dir, ".profile"), []byte("student edit\n"), 0o644))
	emptyLs := "#!/bin/sh\nexit 0\n"

	out, err := runVolumeInitWithStub(suite, buildVolumeInitCommand(nil, "", selfRunUid()), dir, "ls", emptyLs)

	suite.NoError(err, out)
	suite.NotContains(out, volumeInitRefusedMarker, "a file already in place is not a failure")
	got, readErr := os.ReadFile(filepath.Join(dir, ".profile"))
	suite.Require().NoError(readErr)
	suite.Equal("student edit\n", string(got), out)
	bashrc, readErr := os.ReadFile(filepath.Join(dir, ".bashrc"))
	suite.Require().NoError(readErr)
	suite.Equal(seedBashrc, string(bashrc))
	suite.ElementsMatch([]string{".profile", ".bashrc"}, entryNames(suite, dir), "no temporary file left: %s", out)
}

// Another start of the same student is seeding: the volume holds only its
// temporary file. The seed waits; the other one fails and cleans up; the seed
// then finds the volume empty and writes the files.
func (suite *ContainerTestSuite) TestVolumeInitSeedWaitsForAnotherSeedInProgress() {
	defer withSeed(true)()
	dir := suite.T().TempDir()
	other := filepath.Join(dir, seedTempPrefix+".other1")
	suite.Require().NoError(os.WriteFile(other, []byte("half"), 0o600))

	run := exec.Command("sh", "-c", strings.ReplaceAll(buildVolumeInitCommand(nil, "", selfRunUid()), MOUNT_TARGET, dir))
	var out strings.Builder
	run.Stdout, run.Stderr = &out, &out
	suite.Require().NoError(run.Start())
	time.Sleep(1500 * time.Millisecond)
	suite.Require().NoError(os.Remove(other))
	err := run.Wait()

	suite.NoError(err, out.String())
	suite.NotContains(out.String(), volumeInitRefusedMarker)
	suite.ElementsMatch([]string{".profile", ".bashrc"}, entryNames(suite, dir), out.String())
}

// A temporary file left by a volume-init killed mid-step is removed after the
// wait, and the volume is seeded.
func (suite *ContainerTestSuite) TestVolumeInitSeedRemovesAnAbandonedTemporaryFile() {
	defer withSeed(true)()
	savedWait := seedWaitSeconds
	defer func() { seedWaitSeconds = savedWait }()
	seedWaitSeconds = 1
	dir := suite.T().TempDir()
	suite.Require().NoError(os.WriteFile(filepath.Join(dir, seedTempPrefix+".dead01"), []byte("half"), 0o600))

	out, err := runVolumeInit(suite, buildVolumeInitCommand(nil, "", selfRunUid()), dir)

	suite.NoError(err, out)
	suite.NotContains(out, volumeInitRefusedMarker)
	suite.ElementsMatch([]string{".profile", ".bashrc"}, entryNames(suite, dir), out)
}

// An abandoned temporary file the step cannot remove is reported: left in place,
// it keeps the volume from being seeded or copied into.
func (suite *ContainerTestSuite) TestVolumeInitSeedReportsAnAbandonedTemporaryItCannotRemove() {
	defer withSeed(true)()
	savedWait := seedWaitSeconds
	defer func() { seedWaitSeconds = savedWait }()
	seedWaitSeconds = 1
	dir := suite.T().TempDir()
	suite.Require().NoError(os.WriteFile(filepath.Join(dir, seedTempPrefix+".dead01"), []byte("half"), 0o600))

	out, err := runVolumeInit(suite, buildVolumeInitCommand(nil, "", selfRunUid()), dir, "rm")

	suite.NoError(err, out)
	suite.Contains(out, volumeInitRefusedMarker+"seed exit 1: rm: refused by test stub")
	suite.Contains(out, volumeInitWalkMarker, "the walk still runs")
}

// The other seed published first, so this seed's temporary file stays behind
// after the rename without overwrite; a failed removal of it is reported.
func (suite *ContainerTestSuite) TestVolumeInitSeedReportsATemporaryItCannotRemoveAfterPublishing() {
	defer withSeed(true)()
	dir := suite.T().TempDir()
	suite.Require().NoError(os.WriteFile(filepath.Join(dir, ".profile"), []byte("student edit\n"), 0o644))
	stubs := suite.T().TempDir()
	for tool, script := range map[string]string{
		"ls": "#!/bin/sh\nexit 0\n",
		"rm": "#!/bin/sh\necho \"rm: refused by test stub\" >&2\nexit 1\n",
	} {
		suite.Require().NoError(os.WriteFile(filepath.Join(stubs, tool), []byte(script), 0o755))
	}
	run := exec.Command("sh", "-c", strings.ReplaceAll(buildVolumeInitCommand(nil, "", selfRunUid()), MOUNT_TARGET, dir))
	run.Env = append(os.Environ(), "PATH="+stubs+":"+os.Getenv("PATH"))
	raw, err := run.CombinedOutput()
	out := string(raw)

	suite.NoError(err, out)
	suite.Contains(out, volumeInitRefusedMarker+"seed exit 1: rm: refused by test stub")
	got, readErr := os.ReadFile(filepath.Join(dir, ".profile"))
	suite.Require().NoError(readErr)
	suite.Equal("student edit\n", string(got), out)
}

// A student file whose name only looks like a temporary file is student data:
// the volume is left alone at once, nothing removed, no wait.
func (suite *ContainerTestSuite) TestVolumeInitSeedKeepsAStudentFileThatLooksLikeATemporary() {
	defer withSeed(true)()
	dir := suite.T().TempDir()
	for _, name := range []string{".seed.notes", seedTempPrefix + ".notes.txt"} {
		suite.Require().NoError(os.WriteFile(filepath.Join(dir, name), []byte("mine"), 0o600))
	}

	started := time.Now()
	out, err := runVolumeInit(suite, buildVolumeInitCommand(nil, "", selfRunUid()), dir)

	suite.NoError(err, out)
	suite.Less(time.Since(started), 3*time.Second, "no wait on a volume holding student data")
	suite.ElementsMatch([]string{".seed.notes", seedTempPrefix + ".notes.txt"}, entryNames(suite, dir), out)
}

// Another seed has renamed its .profile but not yet its .bashrc: the seed waits
// for it instead of taking the volume as done.
func (suite *ContainerTestSuite) TestVolumeInitSeedWaitsForAHalfPublishedSeed() {
	defer withSeed(true)()
	dir := suite.T().TempDir()
	suite.Require().NoError(os.WriteFile(filepath.Join(dir, ".profile"), []byte(seedProfile), 0o644))
	temp := filepath.Join(dir, seedTempPrefix+".abc123")
	suite.Require().NoError(os.WriteFile(temp, []byte(seedBashrc), 0o644))

	run := exec.Command("sh", "-c", strings.ReplaceAll(buildVolumeInitCommand(nil, "", selfRunUid()), MOUNT_TARGET, dir))
	var out strings.Builder
	run.Stdout, run.Stderr = &out, &out
	suite.Require().NoError(run.Start())
	time.Sleep(1500 * time.Millisecond)
	suite.Require().NoError(os.Rename(temp, filepath.Join(dir, ".bashrc")))
	started := time.Now()
	err := run.Wait()

	suite.NoError(err, out.String())
	suite.NotContains(out.String(), volumeInitRefusedMarker)
	suite.Less(time.Since(started), 3*time.Second, "the wait ends once the other seed is done")
	suite.ElementsMatch([]string{".profile", ".bashrc"}, entryNames(suite, dir), out.String())
}

func (suite *ContainerTestSuite) TestSeedFilesParseInBash() {
	bash, err := exec.LookPath("bash")
	if err != nil {
		suite.T().Skip("no bash on this host")
	}
	for name, content := range map[string]string{".profile": seedProfile, ".bashrc": seedBashrc} {
		check := exec.Command(bash, "-n")
		check.Stdin = strings.NewReader(content)
		out, checkErr := check.CombinedOutput()
		suite.NoError(checkErr, "%s: %s", name, out)
	}
}

// An interactive bash started in a seeded home runs the conda hook when conda is
// on the PATH, as the Jupyter stack images' own .bashrc did.
func (suite *ContainerTestSuite) TestSeedBashrcRunsTheCondaHook() {
	bash, err := exec.LookPath("bash")
	if err != nil {
		suite.T().Skip("no bash on this host")
	}
	if _, err := os.Stat("/usr/local/bin/_activate_current_env.sh"); err == nil {
		suite.T().Skip("this host has the micromamba activation script, which takes precedence")
	}
	home := suite.T().TempDir()
	suite.Require().NoError(os.WriteFile(filepath.Join(home, ".bashrc"), []byte(seedBashrc), 0o644))
	stubs := suite.T().TempDir()
	conda := "#!/bin/sh\n[ \"$1 $2\" = \"shell.bash hook\" ] && echo 'echo conda hook ran'\n"
	suite.Require().NoError(os.WriteFile(filepath.Join(stubs, "conda"), []byte(conda), 0o755))

	run := exec.Command(bash, "-i", "-c", "true")
	run.Env = append(os.Environ(), "HOME="+home, "PATH="+stubs+":"+os.Getenv("PATH"))
	out, _ := run.CombinedOutput()

	suite.Contains(string(out), "conda hook ran")
}
