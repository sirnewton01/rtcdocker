# This docker file builds a DB2 Express C version 10.1 database for x86 64-bit.
# It may work for other configurations with some customization.
# The final database has these parameters:
#
# TCP/IP Port: 50000 (exposed to other containers that are linked)
# Instance: db2inst1
# Password: db2inst1
# Databases: rtc, jts
#

FROM ubuntu:14.04.2
MAINTAINER Chris McGee <cbmcgee@ca.ibm.com>
ADD *expc*.tar.gz /cache
RUN dpkg --add-architecture i386
RUN apt-get update -y
RUN apt-get install libstdc++6:i386 libpam0g:i386 binutils libaio1 -y
RUN apt-get install libstdc++6 libpam0g binutils libaio1 -y
RUN /cache/expc/db2_install -b /opt/ibm/db2/V10.5
RUN rm -fr /cache
RUN useradd -m db2inst1
RUN echo "db2inst1:db2inst1" | chpasswd
ADD setup.sh /
RUN chmod u+x /setup.sh

EXPOSE 50000
CMD ["/setup.sh"]

