This is a dockerfile to assemble a Jazz Build Engine image for running
RTC builds. It can be further extended for additional tooling such as
compilers and deployment tools (see the bluemix subdirectory as an example
that adds BlueMix deployment command line tools).

In order to use this image you will need to unzip an RTC-BuildSystem zip file
retrieved from an RTC build or from jazz.net/downloads into the current directory.
It will create a subdirectory called "jazz" that is used in this dockerfile.
