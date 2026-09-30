#!/bin/bash
set -e
set -u

function create_user_and_database() {
	local database=$1
	echo "  Creating database '$database'"
	psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
	    CREATE DATABASE $database;
	    GRANT ALL PRIVILEGES ON DATABASE $database TO $POSTGRES_USER;
EOSQL
}

if [ -n "${POSTGRES_MULTIPLE_DATABASES:-eventgaze}" ]; then
	echo "Database creation requested: ${POSTGRES_MULTIPLE_DATABASES:-eventgaze}"
	for db in $(echo "${POSTGRES_MULTIPLE_DATABASES:-eventgaze}" | tr ',' ' '); do
		create_user_and_database $db
	done
	echo "Database initialization complete"
fi
