#!/bin/bash

if [ "$RTC_HOSTNAME" == "" ]; then
	export RTC_HOSTNAME=localhost
fi

echo "Setting the liberty/tomcat hostname to $RTC_HOSTNAME"

# TODO set hostname on tomcat
if [ -e "/ccmserver/server/liberty" ]; then
        # Add the hostname to the liberty server configuration file so
        #  that SSL certs among other things know the hostname
        sed -i "s#/featureManager>\$#/featureManager>\n\t<variable name=\"defaultHostName\" value=\"$RTC_HOSTNAME\"/>#g" /ccmserver/server/liberty/clmServerTemplate/server.xml
	unzip /ccmserver/server/liberty/clmServerTemplate/apps/ccm.war.zip WEB-INF/eclipse/configuration/config.ini
	echo osgi.console=8050 >> WEB-INF/eclipse/configuration/config.ini
	echo osgi.console=8050 >> /ccmserver/server/liberty/clmServerTemplate/apps/ccm.war/WEB-INF/eclipse/configuration/config.ini
	zip /ccmserver/server/liberty/clmServerTemplate/apps/ccm.war.zip -u WEB-INF/eclipse/configuration/config.ini
else
	echo "Ooops, is this tomcat?"
	unzip /ccmserver/server/tomcat/webapps/ccm.war WEB-INF/eclipse/configuration/config.ini
	echo osgi.console=8050 >> WEB-INF/eclipse/configuration/config.ini
	zip /ccmserver/server/tomcat/webapps/ccm.war -u WEB-INF/eclipse/configuration/config.ini
fi

# This install is finished, rename the script so that it doesn't get execute
#  next time
mv /install.sh /install-backup.sh

