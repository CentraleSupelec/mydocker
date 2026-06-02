# MyDocker - CentraleSupélec's virtual lab platform
[![SWH](https://archive.softwareheritage.org/badge/swh:1:dir:b39e55f13fd1f808cc87cc73d1e4401cbaf69bf9/)](https://archive.softwareheritage.org/swh:1:dir:b39e55f13fd1f808cc87cc73d1e4401cbaf69bf9;origin=https://github.com/CentraleSupelec/mydocker;visit=swh:1:snp:80a7fd8b4ba2f7c3e03357b1f5a32d8f21e83ae5;anchor=swh:1:rev:920ff5d51de5125f84335bd8390536e7e0d75b37)

A scalable virtual labs provider based on Docker with simple integration to your LMS (Learning Management System).

## Description
Easily give your students access to hundreds of labs based on Docker, directly from your LMS (such as Moodle) in a scalable infrastructure.

With MyDocker :
- you are free to schedule labs whenever you want (by using cloud computing resourses), and free to update you labs' environements without having to ask support from the IT team (your environements are indepedent from others and thus will not cause any conflict)
- you give access to the same lab environement with the same performance to the students
- you keep your students data private and do not leak personnal information

![Descriptive video](docs/assets/video-students.gif)

## Project architecture

The project is splitted between multiple directories :

- [**front**](front) : The static website frontend, built with Angular
- [**back**](back) : The backend retrieving data from the database, and calling the project's Docker API with Protobuf
- [**docker-api**](docker-api) : The custom API server written in Go, wich call the native Docker api
- [**volume-plugin**](volume-plugin) : A fork from the [wetopi docker volume plugin for rbd](https://github.com/wetopi/docker-volume-rbd) enhanced to support either local filesystem or rbd storage

## Diagram

```mermaid
graph TB
    Front[Front - Angular]
    Back[Back - Java Spring]
    DB[(Database - PostgreSQL)]
    DockerAPI[Docker API - Go]
    Ceph([Ceph - RBD Storage])
    SwarmManager[[Swarm manager node]]
    SwarmWorkers[[Swarm workers nodes]]
    Container[[Container w/ JupyterLab, VSCode, ...]]
    Volume[/RBD docker volume/]
    NFS[/NFS local mount/]
    Users((Users))

    Front-->Back
    Back-->DB
    Back-->DockerAPI
    DockerAPI-->Ceph
    DockerAPI-->SwarmManager

    SwarmManager-->Ceph
    SwarmManager-->SwarmWorkers
    SwarmWorkers-->Ceph
    SwarmWorkers-->Container
    SwarmWorkers-->NFS
    Ceph-->Volume
    Container-->Volume
    Container-->NFS

    Users-->|direct web access or via LMS/LTI|Front
    Users-->|http/ssh/novnc/...|Container
```

## Documentation

The documentation is available on our [Github project's page](https://centralesupelec.github.io/mydocker).

To edit the documentation, you can serve it localy : `docker run --rm -it -p 8000:8000 -v ${PWD}:/docs squidfunk/mkdocs-material`

The Github's CI will deploy it once merged.
