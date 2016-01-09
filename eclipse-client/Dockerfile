FROM dorowu/ubuntu-desktop-lxde-vnc

COPY keytool-trust.sh /
COPY startup2.sh /
COPY jazz /jazz
COPY com.ibm.team.eclipse.setup.site /com.ibm.team.eclipse.setup.site
# Bump up screen resolution to 1280x1024x16
RUN sed -i 's/1024x768x16/1280x1024x16/g' /etc/supervisor/conf.d/supervisord.conf
RUN apt-get -y update && apt-get -y install x11-xserver-utils curl
RUN chmod a+rx /startup2.sh
RUN chmod a+rx /keytool-trust.sh

ENTRYPOINT []
CMD /startup2.sh
