# Change this base image to match the image you want to patch
FROM rtc-oracle:501 

# File Author / Maintainer
MAINTAINER Chris McGee <cbmcgee@ca.ibm.com>

EXPOSE 9443

CMD /docker-launch.sh

RUN mkdir -p /ccmserver/server/patch
ADD server-patches/* /ccmserver/server/patch/

