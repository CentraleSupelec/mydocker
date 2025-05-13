package main

import (
	"archive/zip"
	"bufio"
	b64 "encoding/base64"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"math/rand"
	"net/http"
	"net/http/httputil"
	"os"
	"path/filepath"
	"strings"
	"time"

	pb "github.com/centralesupelec/mydocker/docker-api/protobuf"
	"github.com/docker/distribution/reference"
	"github.com/docker/docker/api/types"
	log "github.com/sirupsen/logrus"
	"mvdan.cc/sh/syntax"
)

var letterRunes = []rune("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890")

func randPassword(n int) string {
	b := make([]rune, n)
	for i := range b {
		b[i] = letterRunes[rand.Intn(len(letterRunes))]
	}
	return string(b)
}

func Unzip(r *zip.Reader, dest string) ([]string, error) {
	var filenames []string

	for _, f := range r.File {
		fpath := filepath.Join(dest, f.Name)
		if !strings.HasPrefix(fpath, filepath.Clean(dest)+string(os.PathSeparator)) {
			return filenames, fmt.Errorf("%s: illegal file path", fpath)
		}

		filenames = append(filenames, fpath)

		if f.FileInfo().IsDir() {
			os.MkdirAll(fpath, os.ModePerm)
			continue
		}

		if err := os.MkdirAll(filepath.Dir(fpath), os.ModePerm); err != nil {
			return filenames, err
		}

		outFile, err := os.OpenFile(fpath, os.O_WRONLY|os.O_CREATE|os.O_TRUNC, f.Mode())
		if err != nil {
			return filenames, err
		}

		rc, err := f.Open()
		if err != nil {
			return filenames, err
		}

		_, err = io.Copy(outFile, rc)

		outFile.Close()
		rc.Close()

		if err != nil {
			return filenames, err
		}
	}
	return filenames, nil
}

func readLogs(logReader io.ReadCloser) (string, error) {
	logsBuilder := new(strings.Builder)
	rd := bufio.NewReader(logReader)
	for {
		line, err := readBytesWithTimeout(rd)
		if err != nil && err != io.EOF {
			return "", err
		}
		if len(line) > 8 {
			// Docker API add a 8 bytes headers at the beginning of each line
			// https://github.com/moby/moby/issues/7375#issuecomment-51462963
			logsBuilder.Write(line[8:])
		}
		if err == io.EOF {
			break
		}
	}
	return logsBuilder.String(), nil
}

func readBytesWithTimeout(rd *bufio.Reader) ([]byte, error) {
	dataStream := make(chan []byte, 1)
	errStream := make(chan error)
	duration, err := time.ParseDuration(c.LogsTimeout)
	if err != nil {
		log.Warnf("Unable to parse logs timeout : %v, defaulting to 500ms", err)
		duration, _ = time.ParseDuration("500ms")
	}
	go func(rd *bufio.Reader, dataStream chan []byte, errStream chan error) {
		line, err := rd.ReadBytes('\n')
		if err != nil {
			errStream <- err
			return
		}
		dataStream <- line
		return
	}(rd, dataStream, errStream)

	for {
		select {
		case data := <-dataStream:
			return data, nil
		case readErr := <-errStream:
			return nil, readErr
		case <-time.After(duration):
			return nil, io.EOF
		}
	}
}

func buildServiceCreateOptions(image string) types.ServiceCreateOptions {
	createOptions := types.ServiceCreateOptions{}
	authString := buildRegistryCredential(image)
	log.Debugf("Auth string : '%s'", authString)
	if authString != "" {
		createOptions.QueryRegistry = true
		createOptions.EncodedRegistryAuth = authString
	}
	return createOptions
}

func buildRegistryCredential(image string) string {
	matchingPullCredential := findMatchingCredential(image)
	if matchingPullCredential == nil {
		return ""
	}
	authJson := fmt.Sprintf(
		"{\"username\": \"%s\", \"password\": \"%s\", \"serveraddress\": \"%s\"}",
		matchingPullCredential.Username,
		matchingPullCredential.Password,
		matchingPullCredential.Address,
	)
	return b64.StdEncoding.EncodeToString([]byte(authJson))
}

func findMatchingCredential(image string) *registryCredential {
	parsedReference, err := reference.ParseNormalizedNamed(image)
	if err != nil {
		log.Errorf("%s not a valid image reference", image)
		return nil
	}
	parsedDomain := reference.Domain(parsedReference)
	for _, credential := range c.RegistryCredentials {
		if credential.Address == parsedDomain {
			return &credential
		}
	}
	return nil
}

func convertRequestPortToResponsePort(port *pb.RequestPort) *pb.ResponsePort {
	return &pb.ResponsePort{
		Description:               port.Description,
		ConnexionType:             port.ConnexionType,
		PortToMap:                 port.PortToMap,
		MapTo:                     0,
		RequiredToAccessContainer: port.RequiredToAccessContainer,
	}
}

func createContainerName(userId string, courseId string) string {
	return userId + "-" + courseId
}

func createAdminContainerName(courseId string) string {
	return fmt.Sprintf("%s-admin", courseId)
}

type containerInfo struct {
	userId   string
	courseId string
	isAdmin  bool
}

func parseContainerName(containerName string) (*containerInfo, error) {
	split := strings.Split(containerName, "-")
	if len(split) != 2 {
		return nil, errors.New("Invalid container name")
	}
	if split[1] == "admin" {
		return &containerInfo{userId: "", courseId: split[0], isAdmin: true}, nil
	}
	return &containerInfo{userId: split[0], courseId: split[1], isAdmin: false}, nil
}

func createStudentVolumeName(userId string) string {
	return "student-" + userId
}

var httpClient = &http.Client{Timeout: 10 * time.Second}

func getJson(url string, target interface{}, catch404 bool) error {
	r, err := httpClient.Get(url)
	if err != nil {
		return err
	}
	dump, err := httputil.DumpResponse(r, true)
	log.Tracef("GET Response for %s : %s", url, dump)
	if err != nil {
		return err
	}
	if r.StatusCode == 404 && catch404 {
		return nil
	}
	defer r.Body.Close()
	return json.NewDecoder(r.Body).Decode(target)
}

func RandomString(n int) string {
	var letters = []rune("abcdefghijklmnopqrstuvwxyz")

	s := make([]rune, n)
	for i := range s {
		s[i] = letters[rand.Intn(len(letters))]
	}
	return string(s)
}

func findStringInListCaseInsensitive(toSearch string, existingStrings []string) bool {
	for _, existingString := range existingStrings {
		if strings.ToLower(existingString) == strings.ToLower(toSearch) {
			return true
		}
	}
	return false
}

func allMapValuesZero(input map[string]int64) bool {
	for _, value := range input {
		if value != 0 {
			return false
		}
	}
	return true
}

func wordPartToPosition(wordPart syntax.WordPart, start bool, wordContainsOnlyOnePart bool) int {
	switch x := wordPart.(type) {
	case *syntax.DblQuoted:
		offset := 0
		if x.Dollar && start {
			offset += 1
		}

		// To know if we include the double quotes or not
		// Example 1: `sh -c "start.sh -test=true"` => we don't want to inclue the double quotes in the extracted part
		// Example 2: `start.sh -var="test"` => we want to inclue the double quotes in the extracted part
		if !wordContainsOnlyOnePart {
			offset += 1
		}
		if start {
			return wordPartToPosition(x.Parts[0], true, len(x.Parts) == 1) - offset
		} else {
			return wordPartToPosition(x.Parts[len(x.Parts)-1], false, len(x.Parts) == 1) + offset
		}
	default:
		if start {
			return int(x.Pos().Offset())
		} else {
			return int(x.End().Offset())
		}
	}
}

func commandToParts(command string) []string {
	var result []string
	p := syntax.NewParser()
	in := strings.NewReader(command)
	p.Words(in, func(w *syntax.Word) bool {
		start := wordPartToPosition(w.Parts[0], true, len(w.Parts) == 1)
		end := wordPartToPosition(w.Parts[len(w.Parts)-1], false, len(w.Parts) == 1)
		result = append(result, command[start:end])

		return true
	})
	return result
}
