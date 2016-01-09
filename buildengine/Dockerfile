# Set the base image to Ubuntu
FROM ubuntu:14.04.2

# File Author / Maintainer
MAINTAINER Chris McGee <cbmcgee@ca.ibm.com>

# Default to the JKE banking example build engine on localhost with an
#  aggressive polling interval of 30 seconds (good for demos)
ENV RTC_HOSTNAME localhost
ENV BUILD_USER build
ENV BUILD_PASSWORD build
ENV BUILD_ENGINE jke.dev.engine
ENV SLEEP_TIME 30

COPY launch.sh /launch.sh
RUN chmod a+x /launch.sh

# Run the build engine directly with the Java VM so that we are immune to 64/32-bit
CMD /launch.sh

# Install necessary tools to unzip, proxy and verify that app contexts are initialized
RUN apt-get update -y && apt-get install -y unzip balance curl zip openjdk-7-jre-headless && apt-get clean

COPY jazz /jazz

