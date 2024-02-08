# MyDocker - Back

## Technical Stack

This project was developed with:

- Java 17 + Spring (BackEnd)
- PostgreSQL (Database)
- Docker Swarm (Environments created)

On PostgreSQL, `citext` extension must be installed:

```shell
docker-compose exec postgres psql -U thuv -c 'CREATE EXTENSION IF NOT EXISTS citext;'
```

## How to create a local environment

### Config File

If you need to customize the config, use a Spring profile `dev` :
1. create a file `src/main/resources/application-dev.properties`
2. overwrite needed properties : 
   1. `app.cas.service` must be set to the front URL (If you launched the front locally with the help of the README of the front repository, it must be http://localhost:4200/loginAccept)
   2. `app.cas.rscUrl` must be set to the URL of the CAS you use and you must use the same CAS in the front and the back apps. If you use the CAS launched with docker compose, then the correct url is http://localhost:9443/cas/p3/serviceValidate
   3. You can add somme logging, for instance
   ```properties
   go.debug = true
   
   logging.level.fr.centralesupelec.thuv=DEBUG
   logging.level.io.grpc=ERROR
   logging.level.org.liquibase=DEBUG
   logging.level.io.sentry=DEBUG
   logging.level.org.springframework.security=DEBUG
   ```

### Running the app
1. Execute `docker-compose up -d` to start containers.
2. Generate [your keys](#generate-keys)
3. You can now compile the .jar file : `./mvnw package`, and it builds in `target/thuv.jar`.
4. Run `java -Dspring.profiles.active=dev -jar target/thuv.jar`

### Make the first user an admin
1. Once both back-end and front-end apps are running, visit 'http://localhost:4200'
2. Connect with default user : username is `casuser` and password is `Mellon`
3. In the terminal, run `docker-compose exec postgres psql -U thuv -c "UPDATE users_roles SET role_id = (SELECT id FROM roles WHERE name = 'ROLE_ADMIN') WHERE user_id = 1;"`
4. Refresh http://localhost:4200

## Generate keys

```shell
# For MacOS / Ubuntu 21 and older / Debian 11 and older
    openssl genrsa -out src/main/resources/private_key.pem 4096
    openssl rsa -pubout -in src/main/resources/private_key.pem -out src/main/resources/public_key.pem
# Else
    openssl genrsa -traditional -out src/main/resources/private_key.pem 4096
    openssl rsa -traditional -pubout -in src/main/resources/private_key.pem -out src/main/resources/public_key.pem
```

## Migrations

You need to compile with changes you made then run: `./mvnw liquibase:diff` to generate a changelog, then rename it and add it to the `master.xml` file.

In order to update your database, you can use `./mvnw liquibase:update` or start the application.
For the first use, you need to launch `./mvnw liquibase:changelogSync` in order to sync database and changelog.

## Configure Moodle (LTI integration)

1. Check that `lti.base-path` Java property is the base URL for the backend (for instance `https://example.com/api`). It will be used to generate absolute URIs.
2. In Moodle, go to `Site Administration > Plugins > External tool > Manage tools`. In `Tool URL` text box, type the URL of the front followed by `/lti/register` (for instance `https://example.com/lti/register`) and click "Add LTI Advantage".
3. It should open an iFrame in which you must connect *as an admin*, then the tool is automatically registered in Moodle and the platform is saved in MyDocker database.
4. In MyDocker, get the "invitation link" for the course you are interested in.
5. In Moodle, edit the section of the course you'd like to link to MyDocker, (turn on Editing mode and) click "Add an activity or resource"
6. Click "External tool"
7. Set any name for the activity, select Preconfigured tool "automatic based on tool URL", and enter as the "Tool URL" the "invitation link" fetched earlier.
8. Click "Save and display", you should be redirected to MyDocker logged in as the same user as in Moodle.
