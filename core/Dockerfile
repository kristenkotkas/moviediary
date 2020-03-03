FROM postgres:12

COPY src/main/resources/changelog/01-tables.sql /docker-entrypoint-initdb.d/01.sql
COPY src/main/resources/changelog/02-functions.sql /docker-entrypoint-initdb.d/02.sql
COPY src/main/resources/changelog/03-triggers.sql /docker-entrypoint-initdb.d/03.sql
