# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html).

## Unreleased
### Fixed
- Read key files as stream instead of file to allow reading from JAR

## 2.25.0
### Changed
- Prevent session editing if a deployment is set up

## 2.24.0
### Changed
- Expose deployments in courses list

### Fixed
- Fix courses and dockerimages list for owners

## 2.23.1
### Fixed
- Exclude relations from hashing in ComputeType

## 2.23.0
### Changed
- Upgrade to Java 17 and upgrade dependencies

## 2.22.0
### Added
- Retrieve Node IP once connection is successful through gRPC

## 2.21.2
### Changed
- Use client id (audience) instead of issuer to identify LTI tool deployment

## 2.21.1
### Added
- Add "skipped" status for deployments

## 2.21.0
### Added
- Support autoscaling across regions

## 2.20.0
### Added
- Log different kind of actions (login, container creation, etc. See [reference](src/main/java/fr/centralesupelec/thuv/activity_logging/model/LogAction.java))

### Fixed
- Fix synchronization problem in connection test

## 2.19.2
### Fixed
- Retrieve Terraform instances recursively

## 2.19.1
### Added
- Configure manually added nodes for autoscaling

## 2.19.0
### Added
- Specify compute type / owner for infra scheduled deployment

### Fixed
- Fix creating compute type with empty scaling data

## 2.18.1
### Fixed
- Do not try to fetch course if courseId is not a number
- Add possible grpc debug (for logging level debug and if `go.debug` application property is `true`)

## 2.18.0
### Added
- Add autoscaling config in compute types model. Config is sent to the Docker Microservice to start the crons for autoscaling
- Fix duplicate images in image list when images are shared to at least two people
- Increase connection timeout to ~30minutes for courses on compute types with GPU and no idle worker

## 2.17.1
### Fixes
- Add missing migration

## 2.17.0
### Added
- Add settings to schedule automatic shutdown and warning before shutdown of environments
- Store compute types in database

### Changed
- Store email case-insensitive

### Fixes
- Bad SQL query in image retrieval

## 2.16.1
### Changed
- Remove only services for sessions of cleaned courses

## 2.16.0
### Added
- Allow students to shutdown container

## 2.15.0
### Added
- Allow to start deployment manually
- Add selector for workload type : CPU / GPU / GPU on DGX

## 2.14.0
### Added
- Add createdOn on DockerImage
- Add pagination and filter for DockerImageList
- Add documentation to generate key
- Modify local `application.properties` to take keys from `resources`


## 2.13.1
### Fixes
- Fix deployment update time, taking into account deployment start date before now

## 2.13.0
### Added
- Add LTI integration with register/init/launch paths

## 2.12.0
### Added
- Allow downloading context archive

## 2.11.0
### Added
- Add pagination for course
- Display only one time user if they have multiple roles

## 2.10.0
### Added
- Add description on deployment

## 2.9.1
### Fix
- Fix log4j version

## 2.9.0
### Added
- Add CRUD in order to add users

## 2.8.0
### Added
- Send mail in case of error during the deployment

## 2.7.0
### Added
- Add a route to get logs

## 2.6.0
### Added
- Add permission table to store course and docker image permission

## 2.5.2
### Fixed
- Add pagination for deployment status

## 2.5.1
### Fixed
- `joined` course return course with sessions

## 2.5.0
### Added
- Keep login

## 2.4.0
### Added
- Add custom form for course display

## 2.3.0
### Added
- Add Auto scale up feature

## 2.2.1
### Fixed
- Add course status
- Add boolean to submit student work
- Add pattern validation for course title
- Add pattern validation for docker image name

## 2.2.0
### Added
- Add session in courses
- Return session to student
- Verify that user can ask for a container for a session

## 2.1.0
### Fixed
- `lastSaveDate` is now format by front instead of back

### Added
- Add `createdOn` and `updatedOn` date on Courses


## 2.0.1
### Fixed
- Add default value to `requiredToAccessContainer` port value
- Fix send `SaveDataInformation` with null `lastSaveDate` trigger `NullPointerException`

## 2.0.0
### Breaking Change
- Change API to use new front

### Changed
- Wait for connection before sending data to student
- Pass email when saving data

## 1.8.1
### Fixed
- Fix ports editing

## 1.8.0
### Added
- Received build lo from go and store it

### Changed
- Handle multiple ports for course and docker images

## 1.7.4
### Fix
- Replace `\r\n` by `\n` and remove `\r` in DockerFile and WrapperScript

## 1.7.3
### Changed
- Add liquibase migration

## 1.7.2
### Changed
- Send map containing userId and email for an admin containerDto request

## 1.7.1
### Fixed
- Fix sentry handler

## 1.7.0
### Changed
- Allow to configure ressource limit by course

## 1.6.1
### Fixes
- Send workdir size when saving data to initialize the volume at the correct size
- Create service for grpc request, each service handle a lock to have only one request at a time on the async stub

## 1.6.0
### Added
- Add possibility for admin users of building and testing docker image

## 1.5.1
### Fixed
- bad cascade preventing joining course

## 1.5.0
### Changed
- Admin Role now allows to show and edit all courses
- `/courses/` endpoint retrieves all courses and is allowed only for Admins. Former `/courses/` is now at `/courses/joined`, to retrieve courses joined by the current user.

### Fixed
- Cascade Remove has been removed from Course Model, enabling course deletion
- Move injection in constructor
- Do not pass User in the `UserDetails`
- Rewrite `CourseController` request to simplify reading
- Use loombok to simplify Data class

### Added
- A Teacher Role has been added, with the previous permissions of Admin Role : create a new course, show and edit courses created by oneself
- Add `saveData` endpoint to snapshot an existing containerDto volume, using new `saveData` GRPC method
- Add `initAdminContainer` and `getAdminContainer` endpoints to create a containerDto with a file browser against student submissions, using new `getAdminContainer` GRPC method

## 1.4.0
### Added
- Save student workdir in Ceph
