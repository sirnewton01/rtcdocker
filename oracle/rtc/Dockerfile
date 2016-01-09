# TO USE THIS DOCKER FILE YOU NEED TO CUSTOMIZE A FEW THINGS

# 1) Unzip the JDBC driver that matches the Oracle database you are connecting to into "oracle-driver"

# 2) Modify the version below to match the RTC version you want to test
FROM rtc-server

# File Author / Maintainer
MAINTAINER Chris McGee <cbmcgee@ca.ibm.com>

# As usual, expose port 9443 for connections
EXPOSE 9443

# As usual, we use the docker launch script
CMD /docker-launch.sh

# Add the Oracle driver
RUN mkdir /oracle-driver
RUN chmod a+rx /oracle-driver
ADD oracle-driver /oracle-driver

# Set the environment variable for the Oracle JDBC driver
ENV ORACLE_JDBC_DRIVER_FILE /oracle-driver/ojdbc6.jar

# Add a custom params file with details about the oracle database to use for
# CCM
ADD params.properties .
