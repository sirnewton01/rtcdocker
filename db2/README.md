This folder contains two folders, one for a docker image of DB2 Express-C
and one for a version of RTC that is configured to use DB2. When running
the images the RTC container is linked to the DB2 container using the alias
"db2."

     docker run -it --privileged --name=db2-1 db2
     docker run -it -p 9443:9443 --link=db2-1:db2 rtc-db2:501
