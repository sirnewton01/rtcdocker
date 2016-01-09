#!/bin/sh

until java -jar /jazz/buildsystem/buildengine/eclipse/plugins/org.eclipse.equinox.launcher_* -repository "https://${RTC_HOSTNAME}:9443/ccm" -userId ${BUILD_USER} -pass ${BUILD_PASSWORD} -engineId ${BUILD_ENGINE} -sleepTime ${SLEEP_TIME}
do
	echo "Sleeping and trying the build engine again"
	sleep 10
done
