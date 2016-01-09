# Set the base image to the configured RTC server with the data to migrate
FROM rtc:407-snapshot

# File Author / Maintainer
MAINTAINER Chris McGee <cbmcgee@ca.ibm.com>

EXPOSE 9443

CMD /docker-launch.sh

# Unzip the new JTS+CCM zip
RUN mkdir /ccmserver-upgrade
ADD ccmserver /ccmserver-upgrade

# Add an install script that will perform the upgrade
ADD install.sh /
RUN chmod u+x /install.sh

# Modify the startup scripts so that the server blocks as Docker
#  expects that it will. Also, the logs will show up on the standard
#  output, which is a best practice. Finally, the jvm debugging is enabled
#  on port 8000 (not good for prod systems, but this isn't intended for production
#  in its current form). 
RUN sed -i 's/exec "$PRGDIR"\/"$EXECUTABLE" start "$@"/exec "$PRGDIR"\/"$EXECUTABLE" jpda run "$@"/g' /ccmserver-upgrade/server/tomcat/bin/startup.sh

# Modify the log4j properties to log everything to stdout.
# The log4j properties files indicate that this may fill up the disk.
# Since we are focused on dev and testing this should be accetpable.
RUN sed -i 's/rootLogger=.*/rootLogger=WARN, stdout/g' /ccmserver-upgrade/server/conf/ccm/log4j.properties
RUN sed -i 's/rootLogger=.*/rootLogger=WARN, stdout/g' /ccmserver-upgrade/server/conf/jts/log4j.properties

