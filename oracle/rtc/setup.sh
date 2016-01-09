#!/bin/sh

echo "RTC is starting for the first time. Setting it up..."

# Wait until the jts app is available
response=$(curl --write-out %{http_code} --silent --output /dev/null https://localhost:9443/jts)
while [ "$response" -gt 299 ]; do
	        response=$(curl --write-out %{http_code} --silent --output /dev/null https://localhost:9443/jts)
done

# Check that all of the environment variables are set and replace them in the params file
: ${ORACLE_HOST?"Set ORACLE_HOST to the hostname of your Oracle database."}
: ${ORACLE_PORT?"Set ORACLE_PORT to the port number of your Oracle database."}
: ${ORACLE_INSTANCE?"Set ORACLE_INSTANCE to the instance name of your Oracle database."}
: ${CCM_USER?"Set CCM_USER to the user name of the CCM database."}
: ${CCM_PASS?"Set CCM_PASS to the password of your CCM database."}
: ${JTS_USER?"Set JTS_USER to the user name of the JTS database."}
: ${JTS_PASS?"Set JTS_PASS to the password of your JTS database."}

sed -i "s/ORACLE_HOST/${ORACLE_HOST}/g" /params.properties
sed -i "s/ORACLE_PORT/${ORACLE_PORT}/g" /params.properties
sed -i "s/ORACLE_INSTANCE/${ORACLE_INSTANCE}/g" /params.properties
sed -i "s/CCM_USER/${CCM_USER}/g" /params.properties
sed -i "s/CCM_PASS/${CCM_PASS}/g" /params.properties
sed -i "s/JTS_USER/${JTS_USER}/g" /params.properties
sed -i "s/JTS_PASS/${JTS_PASS}/g" /params.properties

./repotools-jts.sh -setup parametersFile=/params.properties || exit 1

echo "Creating standard test users."
./repotools-ccm.sh -importUsers fromFile=/users.csv
sleep 25
/projectcreator

# Remove this script file since the server is configured
mv /setup.sh /setup-backup.sh

echo "Server setup is complete."
