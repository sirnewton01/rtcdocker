#!/bin/bash

# We must run this container in privileged mode. Bail if we aren't.
sysctl kernel.shmmax=18446744073692774399 || echo "This container must be run in privileged mode. Add --privileged parameter and run again."
sysctl kernel.shmmax=18446744073692774399 || exit 1

# If this is the first time we are run then do an install and
#  create the databases
if [ ! -e /home/db2inst1/sqllib ]; then
	touch /home/db2inst1/.profile
	chown db2inst1:db2inst1 /home/db2inst1/.profile
	/opt/ibm/db2/V10.5/instance/db2icrt -u db2inst1 -p 50000 db2inst1 || exit 1
	su - db2inst1 -c "source /home/db2inst1/sqllib/db2profile && \
		db2start && \
		db2set DB2COMM=tcpip && \
		db2 update dbm cfg using SVCENAME 50000 && \
		db2 create database rtc using codeset UTF-8 territory en PAGESIZE 16384 && \
		db2 create database jts using codeset UTF-8 territory en PAGESIZE 16384 && \
		db2stop && \
		db2start" || exit 1
else
	su - db2inst1 -c "source /home/db2inst1/sqllib/db2profile && \
		db2start" || exit 1
fi

echo "Database Running"
tail -f /home/db2inst1/sqllib/db2dump/db2diag.log
su - db2inst1 -c "source /home/db2inst1/sqllib/db2profile && db2stop"

