This is a collection of docker files (and related files) to build docker
images of RTC. The images are designed to be easy to set up with as much
automation as possible. The intended use of these images is for testing
and development.

Images auto-configure themselves the first time you launch a container.
There is no need to run through the setup wizard, create test users
or project area. All of this is done automatically.
 
Images have debugging turned so that you can easily hook in with
eclipse. There is no need to go digging into the tomcat scripts to
find the right spot to turn it on.

New and updated bundles can be deployed to the server using a special
eclipse plugin. Changes to java code can be hot swapped into the running
server.


Getting Started
---------------

The easiest way to get up and running is to use vagrant. This will
set up a virtual machine with docker and launch the containers. All
that is required is that you download the zips you wish to deploy (e.g.
server, client, build engine, database).

If all you want is a server you can download a JTS+CCM linux 64-bit
zip of the RTC server from jazz.net and place it into the fullRtcTest
directory. Once the zip is there you can bring everything up by issuing
a "vagrant up" command in that directory.

After a period of time (usually 30-40 minutes) the server will become
available at https://localhost:9443/ccm with an example project area
(JKE Banking) and users (deb/dev, al/al, bob/bob and build/build).

The vagrant script will provide you with more details once it is finished.

Runing With Oracle/DB2 Database
-------------------------------

It is possible to run a server with either an Oracle XE or DB2 Express-C
database. In the case of DB2, simply download the db2\_v1012\_linuxx64\_expc.tar.gz file and place it in the fullRtcTest directory before running
"vagrant up." The scripts will automatically detect the presence of the DB2
and set up a docker container for it within the vagrant VM.

For Oracle, you will need to download the RedHat Enterprise Linux 64-bit
version of Oracle XE, extract it so that you have a "Disk1" directory and
place that in the fullRtcTest directory. Since RTC doesn't ship with the JDBC
drivers for Oracle you will also need to create a directory called oracle-drivers
and place all of the JDBC driver JARs in there. Like DB2, the scripts will
automatically detect the presence of these directories in order to create
an docker container for Oracle.


Eclipse Client
--------------

If you want to automatically deploy an eclipse client you can place an
RTC Client zip for Linux 64-bit in the fullRtcTest directory. The client
will be run inside a docker container that is accessed with your web browser
through a VNC session at a specific URL (ie. [http://localhost:6080/vnc_auto.html](http://localhost:6080/vnc_auto.html)). The URL is provided after running
"vagrant up."

Using The Debugging Port
------------------------

Various debugging ports are forwarded to your local machine from the
docker containers. For example, port 8000 is the server debugging port.
You can hook up the eclipse remote debugging capability to this port after
the server comes up. Similarly, you can access the eclipse client via port 8099.
Information about these ports are shown at the end of the vagrant script
when you run "vagrant up."


Using The Server Patcher Port
-----------------------------

The server patcher port can be used in conjunction with the eclipse plugin
to patch a large number of bundles or even add new bundles to the server.
When the bundles are patched or added the server is stopped, repotools
is called to add/change any database tables and the server is restarted.
This all happens automatically, but can take a couple of minutes to complete.

The server patcher can be used to install the server-side test plugins and
test harnesses to enable you to run the JUnit tests against a real server!

When the server receives a patch you will be notified in the container's
standard output. When the server has restarted you will see a "Server Started"
message.


OSGi Console
------------

Sometimes, it is useful to access an OSGi console to debug bundle dependency
problems. The client OSGi console can be accessed via telnet on
port 2001.


Docker Access
-------------

Inside the vagrant VM there are various docker containers and images. You
can access docker via a Linux shell in the vagrant VM. In some cases you
can simply run "vagrant ssh" within the fullRtcTest directory to get to
the VM. On Windows, you may need to open virtualbox, right click on the
fullRtcTest VM and choose "Show." From the console window you can login as
vagrant/vagrant.

Once you have a shell prompt you can issue docker commands. For example,
the containers are programmed to dump as much output as possible to standard
out, which means that you can use the "docker logs" command to see many of
the relevant logs for the server, client, build engine and database.


Cleaning Up
-----------

Sometimes, you may want to completely clean up the VM to start fresh or reclaim
disk space on your laptop. You can do this by issuing a "vagrant destroy"
command within the fullRtcTest directory. You will be prompted to confirm
the action and then it will effectively erase the VM and all running containers.


Reprovision
-----------

A full clean up the VM and then reconstruction of the VM can take a long time.
If you want to reprovision more quickly with a different version you can put
the new zips in the fullRtcTest directory, log into the VM using "vagrant ssh"
or the console (as described above) and run "/provision.sh" script. This will
clean up the containers, images and rebuild them from the new zips.

