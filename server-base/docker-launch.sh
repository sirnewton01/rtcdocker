#!/bin/bash

# Run any installation customizations first
if [ -f /install.sh ]; then
	/install.sh
fi

cd /ccmserver/server
./server.startup -debug &
serverPid=$!

# TODO re-enable this once the signals can be trapped from the docker stop command.
# Kill the server and wait for it for finish when terminated
#trap "echo Received kill signal; kill -TERM $serverPid; wait $serverPid" SIGINT SIGTERM SIGHUP EXIT

# Wait for the server to begin accepting connections on the standard port
nc -z localhost 9443
while [ "$?" -ne 0 ]; do
	nc -z localhost 9443
done

# Run the setup script to configure the server if avaialble
if [ -f /setup.sh ]; then
	. /setup.sh
fi

/server-mgr &

wait $serverPid

# Restart request
while [ -f /restart.sh ]; do
	echo "Restarting server..."
	/restart.sh
	rm /restart.sh
	# Tomcat
	if [ ! -e "/ccmserver/server/liberty" ]; then
	        ./server.startup
	fi
	# Liberty
	if [ -e "/ccmserver/server/liberty" ]; then
	        ./server.startup -debug
	fi
done

