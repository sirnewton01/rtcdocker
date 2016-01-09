#!/bin/bash

echo "RTC is starting for the first time. Setting it up..."

if [ "$RTC_HOSTNAME" != "" ]; then
	echo "Setting hostname of this system to $RTC_HOSTNAME"
	echo "127.0.0.1  $RTC_HOSTNAME" >> /etc/hosts
	sed -i "s/localhost/$RTC_HOSTNAME/g" /params.properties
else
	RTC_HOSTNAME=localhost
fi

# Wait until the jts app is available
response=$(curl --write-out %{http_code} --silent --output /dev/null https://${RTC_HOSTNAME}:9443/jts)
while [ "$response" -gt 299 ]; do
	        response=$(curl --write-out %{http_code} --silent --output /dev/null https://${RTC_HOSTNAME}:9443/jts)
done

./repotools-jts.sh -setup parametersFile=/params.properties || exit 1

if [ "$RTC_MTM" != "" ]; then
	echo "Deploying the money that matters (jke) sample"
	/projectcreator -repo=https://$RTC_HOSTNAME:9443/ccm -deployMTM
	./repotools-ccm.sh -importUsers fromFile=/users-jke.csv
else
	echo "Creating standard JUnit test users and blank project area"
	./repotools-ccm.sh -importUsers fromFile=/users.csv
	/projectcreator -repo=https://$RTC_HOSTNAME:9443/ccm
fi

# Remove this script file since the server is configured
mv /setup.sh /setup-backup.sh

echo "Server setup is complete. Server is running."
