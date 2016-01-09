# Change this base image to match the image you want to patch
FROM rtc:502

# File Author / Maintainer
MAINTAINER Chris McGee <cbmcgee@ca.ibm.com>

EXPOSE 9443

CMD /docker-launch.sh

RUN mkdir -p /server-patches
ADD server-patches/* /server-patches/
ADD patch-plugins.sh /

RUN chmod a+x /patch-plugins.sh && sleep 1 && /patch-plugins.sh

