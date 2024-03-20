# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html).

## Unreleased
## 2.24.5
### Added
- deployement_enabled parameter to show/hide deployment related settings

## 2.24.4
### Added
- Automatically start environment when joining a session if computeType is not GPU

## 2.24.3
### Added
- Build ID column to list of image builds

### Fixed
- No builds message while loading image builds

## 2.24.2
### Fixed
- Redirect immediately to CAS
- Keep history of wether port should be displayed when a new image is selected in course edit form
- Fix USER/USERNAME templating in link preview
- Hide empty list message during loading of images

## 2.24.1
### Fixed
- Make the sessions form responsive

## 2.24.0
### Added
- Add link to join link from admin courses list and redirect to available session from join link 

## 2.23.0
### Changed
- Prevent session editing if a deployment is set up

## 2.22.0
### Changed
- Display an alert on course edit page if no deployment is scheduled

## 2.21.0
### Changed
- Change course lists page: change  buttons layout
- Update course lists button in side menu

## 2.20.1
### Fixed
- fix collapsible admin container row
- hide button to restart env in student view

## 2.20.0
### Added
- Configure default load for scheduled deployments

### Fixed
- Add "skipped" status for deployments

## 2.19.0
### Added
- Support autoscaling across regions

## 2.18.2
### Fixed
- Delay deletion at container start

## 2.18.1
### Added
- Configure manually added nodes for autoscaling

## 2.18.0
### Added
- Specify compute type for infra scheduled deployment

## 2.17.1
### Fixed
- memory leak due to timeout if container creation is blocked

## 2.17.0
### Added
- Add scaling configuration in compute types CRUD

## 2.16.0
### Added
- Add settings for automatic shutdown
- CRUD for compute types

### Changed
- Hide auto shutdown toggle

## 2.15.1
### Changed
- Change wording on container connection timeout

## 2.15.0
### Added
- Add button allowing students to shutdown a container

## 2.14.0
### Added
- Add selector for workload type : CPU / GPU / GPU on DGX
- Add search field on docker image list
- Add sort on docker image list
- Add creation date on docker image
- Default sort is createdOn desc
- Display resources on deployment list
- Add button to launch deployment manually

## 2.13.1
### Fix
- Truncate pipe no longer crash when the element is null

## 2.13.0
### Added
- Add lti course choosing

## 2.12.0
### Added
- Add idtoken param in shell path to authenticate with existing jwt
- Add secured LTI register page

## 2.11.0
### Added
- Allow downloading context archive

## 2.10.0
### Added
- Update course list: add pagination and filtering and sortering
- Reload page when student can ask access for a activity

## 2.9.0
### Added
- Add description on deployment

## 2.8.0
### Added
- Add an interface to administrate users

## 2.7.0
### Added
- Add a button to see service logs when asking for a container

## 2.6.0
### Added
- Add course and docker image view, edit and creator permission

## 2.5.1
### Fixed
- Fix display shell with all course

## 2.5.0
### Added
- Add paginator for deployment status
- Do not display midnight hour for students

## 2.4.2
### Fixed
- Use `date-fns` module
- Fix date picker: inverting month and day when writing date manually

## 2.4.1
### Fixed
- Display all course to student if there is no upcoming course

## 2.4.0
### Added
- Add custom form container

## 2.3.0
### Added
- Add Auto scale up feature

## 2.2.1
### Fixed
- Add course status
- Add boolean to submit student work
- Add pattern validation for course title
- Add pattern validation for docker image name
- Fix docker build status when empty array

## 2.2.0
### Added
- Add session in courses
- Display session to the user
- Add a button to create a course from a docker image
