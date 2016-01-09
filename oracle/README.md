Below are two directories that you can use to generate an image for the DB and RTC server
 containers. Follow the instructions in the Docker files to customize them as needed.

Once you are finished you can launch the DB container, provide a name using the "--name" parameter
 and link that to the RTC container using the "--link" parameter. RTC expects to find DB under
 the alias "oracle."

e.g.
docker run -it --name oracle1 oracle-db-img
<wait until the database is up and configured>
docker run -it --link oracle1:oracle -p 9443:9443 oracle-rtc-img

