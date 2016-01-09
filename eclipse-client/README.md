RTC Eclipse client docker image. This image can be used to
set up a pre-canned eclipse client that is ready to connect
to an RTC server.

To build an image, first unzip a Linux 64-bit full client zip
from a build or jazz.net downloads -> RTC -> All Downloads into
the current directory. Then build the image like this:

	$ docker build -t rtc-client .

When you run the plain image it will assume that the server is at
localhost. If you have the server running in another container and
exported port 9443 to the host you can run this image like this:

	$ docker run -it --net=host -p 6080:6080 rtc-client

Once the client image is running you can access the desktop using
your web browser at http://127.0.0.1:6080 . If you are in a bootdocker
or virtualbox/vagrant environment you will need to export port 6080
from the VM to your local host for this to work.

Alternatively, you can target the client to an external RTC server
like this:

	$ docker run -it -p 6080:6080 -e RTC_HOSTNAME=myserver.com rtc-client

It assumes that the server is configured with port 9443 and /ccm context.
If further customization is required then you can change the repository connection
in the Team Artifacts view in Eclipse.
