The dockerfile and images generated from it are intended to be extended.
The base image is only useful for basic smoke testing.

In the image there is a params.properties file at the root of the filesystem.
This file is a customized respose file. You may be able to use it as a template
for your own images. To generate your own response file you can do perform the
following steps.

	docker run -it dockerhub.rtp.raleigh.ibm.com/cbmcgee/rtc:501 bash
	<edit the /setup.sh file, replace parametersFile to responseFile>
	/docker-launch.sh

If there are any active setup steps required for your image you can replace
the /setup.sh file with our own customized version. The image will run it
the first time that someone launches a container and not on subsequent launches.
