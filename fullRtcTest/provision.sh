#!/bin/bash

# The default is to deploy the Money That Matters / JKE sample
# Otherwise, if the "junit" parameter is provided then the JUnit
#  users are added and only a simple project area is created.
SERVER_CONFIG="-e RTC_MTM=true"
CLIENT_CONFIG=()
CLIENT2_CONFIG=(-e RTC_USER=bob RTC_PASSWORD=bob)
JBE_CONFIG="-e BUILD_ENGINE=jke.dev.engine"
JBE2_CONFIG="-e BUILD_ENGINE=jke.production.engine"
EXTRA_CREDENTIALS="deb/deb
bob/bob
marco/marco
build/build"
if [ "$#" -eq 1 ]; then
	if [ "$1" == "junit" ]; then
		SERVER_CONFIG=""
		CLIENT_CONFIG=(-e RTC_USER=TestJazzAdmin1 -e RTC_PASSWORD=TestJazzAdmin1 -e RTC_PROJECT_AREA=ProjectArea1 -e "RTC_STREAM=ProjectArea1 Stream")
		CLIENT2_CONFIG=(-e RTC_USER=TestUser1 -e RTC_PASSWORD=TestUser1 -e RTC_PROJECT_AREA=ProjectArea3 -e "RTC_STREAM=ProjectArea1 Stream")
		JBE_CONFIG="-e BUILD_USER=TestJazzUser1 -e BUILD_PASSWORD=TestJazzUser1 -e BUILD_ENGINE=dev"
		JBE2_CONFIG="-e BUILD_USER=TestJazzUser1 -e BUILD_PASSWORD=TestJazzUser1 -e BUILD_ENGINE=prod"
		EXTRA_CREDENTIALS="TestJazzAdmin2/TestJazzAdmin2
TestJazzUser1/TestJazzUser1
TestJazzUser2/TestJazzUser2"
	fi
fi

# Stop and remove all running containers
echo "Killing all running containers and images"
docker kill $(docker ps -a -q)  2>/dev/null
docker rm $(docker ps -a -q) 2> /dev/null

# Remove any RTC images
docker rmi rtc-client rtc-server rtc-server-db2 rtc-server-oracle rtc-jbe

# Copy all of the docker data over to improve speed of building images
rm -rf /shared/server-base/ccmserver/*
rm -rf /shared/eclipse-client/jazz
rm -rf /shared/buildengine/jazz
rm -rf ~/server-base
rm -rf ~/eclipse-client
rm -rf ~/buildengine
cp -r /shared/server-base ~/server-base
cp -r /shared/eclipse-client ~/eclipse-client
cp -r /shared/buildengine ~/buildengine

GUIDE_MSG="";

USE_DB2="false"
USE_ORACLE="false"
if ls /vagrant/db2*.tar.gz 1> /dev/null 2>&1; then
	USE_DB2="true"
	rm -f /shared/db2/db2*.tar.gz
	rm -rf ~/db2
	cp -r /shared/db2 ~/db2
	if ! docker images | grep db2 >/dev/null 2>&1; then
		cd ~/db2/db && cp /vagrant/db2*.tar.gz . && docker build -t db2 .
	fi
	docker run -d --privileged --name=db2 -p 50000:50000 db2
	GUIDE_MSG="$GUIDE_MSG
Access DB2 at JDBC //localhost:50000/rtc:user=db2inst1;password=db2inst1"
elif ls /vagrant/oracle-driver 1> /dev/null 2>&1 && ls /vagrant/Disk1 1> /dev/null 2>&1; then
	USE_ORACLE="true"
	rm -f /shared/oracle/Disk1
	rm -rf ~/oracle
	cp -r /shared/oracle ~/oracle
	if ! docker images | grep oracle >/dev/null 2>&1; then
		cd ~/oracle/db && cp -r /vagrant/Disk1 . && docker build -t oracle .
	fi
	docker run -d --name=oracle -p 1521:1521 oracle
	GUIDE_MSG="$GUIDE_MSG
Access Oracle at JDBC thin:rtc/rtc@localhost:1521/XE"
fi

# Unzip all of the RTC bits, build and run each image
cd ~/server-base/ccmserver && unzip /vagrant/JTS*.zip && cd ~/server-base && docker build -t rtc-server .
if [ "$USE_DB2" == "true" ]; then
	# We have DB2, let's use it
	cd ~/db2/rtc && sed -i 's/FROM.*/FROM rtc-server/g;' Dockerfile && docker build -t rtc-server-db2 .
	docker run -d --name=server -p 9443:9443 -p 8000:8000 -p 8050:8050 -p 9000:9000 -d $SERVER_CONFIG --link db2:db2 rtc-server-db2
elif [ "$USE_ORACLE" == "true" ]; then
	# We have Oracle, let's use it
	cd ~/oracle/rtc && sed -i 's/FROM.*/FROM rtc-server/g;' Dockerfile
	
	# In this case, the server doesn't have the JDBC driver built-in, copy it over
	cp -r /vagrant/oracle-driver .

	docker build -t rtc-server-oracle .

	docker run -d --name=server -p 9443:9443 -p 8000:8000 -p 8050:8050 -p 9000:9000 -d $SERVER_CONFIG --link oracle:oracle rtc-server-oracle
else
	docker run -d --name=server -p 9443:9443 -p 8000:8000 -p 8050:8050 -p 9000:9000 -d $SERVER_CONFIG rtc-server
fi
GUIDE_MSG="$GUIDE_MSG
Access the server with https://localhost:9443/ccm
    Port 8000 - Java debugger port
    Port 8050 - OSGi console port
    Port 9000 - Server management port (hot swap new/changed bundles)"

if ls /vagrant/RTC-Client-*.zip >/dev/null 2>&1; then
	cd ~/eclipse-client && unzip /vagrant/RTC-Client-Linux64*.zip && docker build -t rtc-client .
	docker run -d --name=client --net=host -p 6080:6080 -p 2001:2001 -p 8099:8099 "${CLIENT_CONFIG[@]}" rtc-client
	#docker run -d --name=client2 --net=host -p 6080:6081 "${CLIENT2_CONFIG[@]}" rtc-client
	GUIDE_MSG="$GUIDE_MSG
Access the primary eclipse client with http://localhost:6080/vnc_auto.html
    Port 8099 - Java debugger port
    Port 2001 - Telnet port for OSGi console"
fi

if ls /vagrant/RTC-BuildSystem-*.zip >/dev/null 2>&1; then
	# TODO expose build engine debug port and provide guide message text
	cd ~/buildengine && unzip /vagrant/RTC-BuildSystem-*.zip && docker build -t rtc-jbe ~/buildengine
	docker run --name=dev -d --net=host $JBE_CONFIG rtc-jbe
	# docker run -d --name=prod --net=host $JBE2_CONFIG rtc-jbe
fi

echo " --------------------------------------------------------------- "
echo "The system is now running. Here are the details:"
echo ""
echo -e "$GUIDE_MSG"
echo ""
echo "To access advanced functions such as logs and docker"
echo " you can access the linux shell using 'vagrant ssh' or"
echo " virtualbox manager by right-clicking on the machine->Show."
echo " Credentials are vagrant/vagrant."
echo "To re-provision new versions of RTC bits you can run /provision.sh"
echo ""
echo "Credentials:"
echo "TestJazzAdmin1/TestJazzAdmin1"
echo "$EXTRA_CREDENTIALS"
echo " ---------------------------------------------------------------- "

