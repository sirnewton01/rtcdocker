# To use this docker file you must first download and unzip the Linux x64 version of Oracle 11g XE from here:
# http://www.oracle.com/technetwork/database/database-technologies/express-edition/downloads/index.html
# Once unzipped it should create a directory Disk1 in this folder
#
# The image has the following parameters:
# Port: 1521
# SID: XE
# sys/system password: oracle
# rtc tablespace password: rtc
# jts tablespace password: jts
#
# NB: you should make sure that your boot2docker VM has 5GB of RAM allocated to it.
# NB: rtc and jts tablespaces are created on first launch of the container.

FROM ubuntu:14.04.2

ADD Disk1 /

RUN apt-get -y update && apt-get -y install alien libaio1 unixodbc vim bc

RUN alien --scripts -d oracle-xe-*.rpm
RUN rm /oracle-xe-*.rpm

ADD chkconfig /sbin/
RUN chmod 755 /sbin/chkconfig
RUN ln -s /usr/bin/awk /bin/awk 
RUN mkdir /var/lock/subsys 
RUN touch /var/lock/subsys/listener

RUN dpkg -i /oracle-xe*.deb
RUN rm /oracle-xe*.deb

ADD init.ora /
ADD initXETemp.ora /

RUN mv /init.ora /u01/app/oracle/product/11.2.0/xe/config/scripts
RUN mv /initXETemp.ora /u01/app/oracle/product/11.2.0/xe/config/scripts
RUN chmod a+r /u01/app/oracle/product/11.2.0/xe/config/scripts/*

RUN printf 8080\\n1521\\noracle\\noracle\\ny\\n | /etc/init.d/oracle-xe configure

RUN echo 'export ORACLE_HOME=/u01/app/oracle/product/11.2.0/xe' >> /etc/bash.bashrc
RUN echo 'export PATH=$ORACLE_HOME/bin:$PATH' >> /etc/bash.bashrc
RUN echo 'export ORACLE_SID=XE' >> /etc/bash.bashrc

EXPOSE 1521
#EXPOSE 8080

ADD makeUser.sh /
RUN chmod 755 /makeUser.sh

#RUN /etc/init.d/oracle-xe start && /makeUser.sh

CMD sed -i -E "s/HOST = [^)]+/HOST = $HOSTNAME/g" /u01/app/oracle/product/11.2.0/xe/network/admin/listener.ora; \
service oracle-xe start; \
/makeUser.sh rtc; \
/makeUser.sh jts; \
echo "Oracle DB ready for connections"; \
tail -f /u01/app/oracle/diag/rdbms/xe/XE/trace/*
