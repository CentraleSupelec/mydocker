# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
this project does NOT adhere to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## Unreleased

### Added
- Reverse proxy to containers

## 2.16.2
### Added
- Manually stop autoscaling by creating a file

## 2.16.1
### Added
- Pass registry credentials to build image

### Fixed
- Build image on node with same volume_backend swarm label

## 2.16.0
### Changed
- Optional disabling of the volume precreation (used only for RBD)
- Put AdminImage in config and rename module

## 2.15.0
### Added
- Add GetNodeIP gRPC Request/Response

## 2.14.2
### Fixed
- Use follow in logs retrieval for build

## 2.14.1
### Fixed
- Use string array for env vars to avoid key lowercase in unmarhsalling

## 2.14.0
### Changed
- Use env vars from config for auth in deploy containers

## 2.13.3
### Added
- More logs

## 2.13.2
### Fixed
- Provision in scale up only if there are missing gpus or previous job has failed
- Fix infinite loop in scaledown if a manual node is idle

## 2.13.1
### Added
- Tests in CI
- Refacto to inject interface of Docker client in scaledown service and scale down taking into account manual idle nodes

## 2.12.1
### Fixed
- Fix logs reading using follow option
- Add "skipped" deployment status

## 2.12.0
### Added
- Support autoscaling across regions
- Send email if autoscaling fails with container logs

## 2.11.1
### Added
- Configure manually added nodes for autoscaling and run autoscaling for idle nodes even if no pending services

## 2.11.0
### Added
- Specify owner for infra scheduled deployment

### Changed
- Do not fail scale down if a swarm node is unreachable

## 2.10.1
### Fixed
- Catch nil pointer for services with no resources

## 2.10.0
### Added
- Add autoscale up and autoscale down using a dedicated terraform state and config received from the Back microservice
- Build images only on Swarm workers with label `volume_backend` equal to `rbd`

## 2.9.0
### Added
- Handle deletion for environments scheduled to be deleted

### 2.8.0
- Use new volume driver centralesupelec/mydockervolume
- Change config.yml format to handle multiple registries credentials and optional authentication

## 2.7.1
### Changed
- Remove only services for sessions of cleaned courses

## 2.7.0
### Added
- Add service to shutdown container

## 2.6.0
### Added
- Add labels constraint and gpu resource handling
 
## 2.5.1
### Added
- Fix volume creation

## 2.5.0
### Added
- Add route to get logs

## 2.4.3
### Fixed
- Fix deploy environment

## 2.4.2
### Fixed
- Merge count if multiple worker have the same region and same flavor

## 2.4.1
### Fixed
- Do not override infra deployment status at the end

## 2.4.0
### Added
- Try to delete service with label `deleteAfter` every 5 minutes if `deletionTime` is passed

## 2.3.1
### Fix
- Activate Kaniko cache

## 2.3.0
### Added
- Can use pregenerate login (used to keep user and password for the same course)

## 2.2.0
### Added
- Auto scale up infra

## 2.1.0
### Added
- Pre create CEPH volumes

## 2.0.1
### Added
- Change random password available runes

## 2.0.0
### Breaking changes
- Update `.proto` schema

## 1.10.0
### Added
- Push build image to different registry specify by `BuildImageRepository` in config
- Save user submission in Ceph FS directory with the name of the student

## 1.9.1
### Fixed
- Pull BuildImage from Harbor registry

## 1.9.0
### Added
- Send build log to back

### Changed
- Handle multiple ports for container creation

## 1.8.3
### Fixed
- Pull SAVE_IMAGE before copying context

## 1.8.2
### Changed
- Use map to associate user_id to email

## 1.8.1
### Changed
- Allow handling subfolder in context archive

## 1.8.0
### Changed
- Allow to configure ressource limit by course

## 1.7.3
### Fixes
- Allow setting incoming message size (default size to 5Go)

## 1.7.2
### Changed
- Authenticate to MyDocker registry to fetch admin image

## 1.7.1
### Fix
- Create submission volume at the correct size

## 1.7.0
### Added
- Add possibility for admin users of building and testing docker image

### Fixes
- Fix save student workdir can throw index out of range error

## 1.6.6
### Fixed
- Set manually the options for submission volume

## 1.6.5
### Fixed
- Find submission volumes only by names because labels do not appear on volume list on Swarm manager 

## 1.6.4
### Fixed
- Disable multi mount protection to allow mounting by save container

## 1.6.3
### Fixed
- Fix data saving by using a service instead of a container

## 1.6.2
### Fixed
- Pull rsync image before creating the container

## 1.6.1
### Fixed
- Fix port retrieval for existing containers

## 1.6.0
### Changed
- Volumes mount on admin containers are now read-only

## 1.5.0
### Added
- Add new GRPC `saveData` method to snapshot an existing volume `{userId}-{courseId}` in an upserted `{userId}-{courseId}-submission` volume
- Add new GRPC `getAdminContainer` method to expose all `{userId}-{courseId}-submission` volumes in a new service with web file browser

## 1.4.0
### Added
- Save student workdir

## 1.3.2
### Fixes
- Return IPs of an available worker

## 1.3.1
### Fixes
- Read config before parsing it

## 1.3.0
### Added
- Support external `config.yml`

## 1.2.0
### Added
- Support HTTP connexion type
- Allow to configure expose port via the protobuf request
