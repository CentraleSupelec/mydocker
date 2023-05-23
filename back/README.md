# MyDocker - Back

### Technical Stack

This project was developed with:

- Java + Spring (BackEnd)
- PostgreSQL (Database)
- Docker Swarm (Environments created)

On PostgreSQL, `citext` extension must be installed:

```shell
docker-compose exec postgres psql -U thuv -c 'CREATE EXTENSION citext;'
```

### Generate keys

```shell
    openssl genrsa -out private_key.pem 4096
    openssl rsa -pubout -in private_key.pem -out public_key.pem
    mv private_key.pem public_key.pem src/main/resources/
```

### Migrations

You need to compile with changes you made then run: `./mvnw liquibase:diff` to generate a changelog, then rename it and add it to the `master.xml` file.

In order to update your database, you can use `./mvnw liquibase:update` or start the application.
For the first use, you need to launch `./mvnw liquibase:changelogSync` in order to sync database and changelog.

### Configure Moodle (LTI integration)

1. Check that `lti.base-path` Java property is the base URL for the backend (for instance `https://example.com/api`). It will be used to generate absolute URIs.
2. In Moodle, go to `Site Administration > Plugins > External tool > Manage tools`. In `Tool URL` text box, type the URL of the front followed by `/lti/register` (for instance `https://example.com/lti/register`) and click "Add LTI Advantage".
3. It should open an iFrame in which you must connect *as an admin*, then the tool is automatically registered in Moodle and the platform is saved in MyDocker database.
4. In MyDocker, get the "invitation link" for the course you are interested in.
5. In Moodle, edit the section of the course you'd like to link to MyDocker, (turn on Editing mode and) click "Add an activity or resource"
6. Click "External tool"
7. Set any name for the activity, select Preconfigured tool "automatic based on tool URL", and enter as the "Tool URL" the "invitation link" fetched earlier.
8. Click "Save and display", you should be redirected to MyDocker logged in as the same user as in Moodle.
