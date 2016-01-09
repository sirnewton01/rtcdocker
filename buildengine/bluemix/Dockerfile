# Set the base image to the JBE
FROM jbe:601-1

# File Author / Maintainer
MAINTAINER Chris McGee <cbmcgee@ca.ibm.com>

ENV BM_API https://api.ng.bluemix.net

# Install docker (client only, not daemon), cf tool and bluemix plugin
RUN curl -L "https://get.docker.com/builds/Linux/x86_64/docker-latest" > /bin/docker && chmod a+x /bin/docker
RUN curl -L "https://cli.run.pivotal.io/stable?release=linux64-binary&source=github-rel" | tar -zxf - && mv /cf /bin
RUN cf install-plugin https://static-ice.ng.bluemix.net/ibm-containers-linux_x64

# Set the default API endpoint to point to BlueMix
RUN cf api ${BM_API}
