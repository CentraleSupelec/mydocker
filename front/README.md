# MyDocker - Front

This project was generated with [Angular CLI](https://github.com/angular/angular-cli) version 12.0.3.

## Installation

Use the Node version indicated in the file .nvmrc. The lockfile is `lockfileVersion 3`, which npm 6
cannot read, so npm 7 or later is required to install at all.

```bash
npm ci
```

The CI pipeline passes `--legacy-peer-deps`. It is not required with npm 10, and the install
succeeds either way, but using the same flag locally keeps the installed tree identical to CI.

## Config file

The file is located in `src/assets/config.js`. The file `config.js.dist` is an example of the syntax to use.
- The back url, if launched with docker compose from the mydockerback repository is http://localhost:8080/ (Be carreful, the last / is important !). Note that it uses http protocol (and not https).
- The front url is basically the url you use to access the application http://localhost:4200. Port 4200 is the default port for Angular application (when you launch `ng serve`)
- CAS login and logout url can point to `cas` container launched with docker compose from the mydockerback repository that listens on port 9443. It is therefore http://localhost:9443/cas/login and http://localhost:9443/cas/login. Be careful, the same CAS url must be specified in front config and in back config.

## Development server

To have a functional development server, you first need to run the backend and the GoAPI. Check these repositories before launching your dev server.

For a dev server, run : 
```
npm run start
```

For a dockerized dev server, run :
```shell
docker-compose up -d
```

Navigate to `http://localhost:4200/`. The app will automatically reload if you change any of the source files.

## Code scaffolding

Run `ng generate component component-name` to generate a new component. You can also use `ng generate directive|pipe|service|class|guard|interface|enum|module`.

## Build

Run `ng build` to build the project. The build artifacts will be stored in the `dist/` directory.

## Running unit tests

Run `ng test` to execute the unit tests via [Karma](https://karma-runner.github.io).

## Running end-to-end tests

Run `ng e2e` to execute the end-to-end tests via a platform of your choice. To use this command, you need to first add a package that implements end-to-end testing capabilities.

## Further help

To get more help on the Angular CLI use `ng help` or go check out the [Angular CLI Overview and Command Reference](https://angular.io/cli) page.
