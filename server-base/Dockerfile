# Set the base image to Ubuntu
FROM ubuntu:14.04.2

# File Author / Maintainer
MAINTAINER Chris McGee <cbmcgee@ca.ibm.com>

EXPOSE 9443

CMD /docker-launch.sh

# Install necessary tools to unzip, proxy and verify that app contexts are initialized
RUN apt-get update -y && apt-get install -y unzip balance curl zip && apt-get clean

# Provide the parameters and docker launcher script
# The launcher script will auto-configure a server using environment variables
ADD params.properties .
ADD docker-launch.sh .
ADD docker-shutdown.sh .
ADD install.sh .
ADD setup.sh .
ADD users.csv .
ADD users-jke.csv .
ADD projectcreator .
ADD server-mgr .
RUN chmod a+x /server-mgr /docker-launch.sh /docker-shutdown.sh /install.sh /setup.sh /projectcreator

# Unzip the JTS+CCM zip
RUN mkdir /ccmserver
ADD ccmserver /ccmserver

# Modify the startup scripts so that the server blocks as Docker
#  expects that it will. Also, the logs will show up on the standard
#  output, which is a best practice. Finally, the jvm debugging is enabled
#  on port 8000 (not good for prod systems, but this isn't intended for production
#  in its current form). 
# Tomcat
RUN if [ ! -e "/ccmserver/server/liberty" ]; then sed -i 's/exec "$PRGDIR"\/"$EXECUTABLE" start "$@"/exec "$PRGDIR"\/"$EXECUTABLE" run "$@"/g' /ccmserver/server/tomcat/bin/startup.sh; fi
RUN if [ ! -e "/ccmserver/server/liberty" ]; then sed -i 's/$CATALINA_HOME\/bin\/catalina.sh jpda start/$CATALINA_HOME\/bin\/catalina.sh jpda run/g' /ccmserver/server/server.startup; fi
# Liberty
RUN if [ -e "/ccmserver/server/liberty" ]; then sed -i 's/suspend=y/suspend=n/g' /ccmserver/server/liberty/wlp/bin/server; fi

# Modify the log4j properties to log everything to stdout.
# The log4j properties files indicate that this may fill up the disk.
# Since we are focused on dev and testing this should be accetpable.
RUN sed -i 's/rootLogger=.*/rootLogger=WARN, stdout/g' /ccmserver/server/conf/ccm/log4j.properties
RUN sed -i 's/rootLogger=.*/rootLogger=WARN, stdout/g' /ccmserver/server/conf/jts/log4j.properties

