# TO USE THIS DOCKER FILE YOU NEED TO CUSTOMIZE A FEW THINGS

# 1) Modify the version below to match the RTC version you want to test
FROM rtc-server

# 2) The image assumes that you will link it with a DB2 database with the alias "db2"
#    e.g. $ docker run --name=db2-1 mydb2image
#         $ docker run --link db2-1:db2 myrtcimage 

# File Author / Maintainer
MAINTAINER Chris McGee <cbmcgee@ca.ibm.com>

# As usual, expose port 9443 for connections
EXPOSE 9443

# As usual, we use the docker launch script
CMD /docker-launch.sh

# Add a custom params file with details about the oracle database to use for
# CCM and JTS (note: the dataware houses remain derby)
ADD params.properties .

