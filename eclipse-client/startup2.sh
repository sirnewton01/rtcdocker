#!/bin/bash

/startup.sh &

# Wait for the xserver to come up
export DISPLAY=:1
while ! xset q &>/dev/null; do
	sleep 10
done

# Add the CLI to the path
echo 'export PATH=$PATH:/jazz/scmtools/eclipse' >> /home/ubuntu/.bashrc

# Install the startup bundle to configure the client
until /jazz/client/eclipse/eclipse -application org.eclipse.equinox.p2.director -repository file:///com.ibm.team.eclipse.setup.site -installIU com.ibm.team.eclipse.setup.feature.feature.group -destination /jazz/client/eclipse
do
	echo "Retrying the provisioning operation of the startup bundle..."
	sleep 2
done

if [ "$RTC_HOSTNAME" != "" ]; then
	echo "Server is at $RTC_HOSTNAME"
else
	RTC_HOSTNAME=localhost
fi

# X sessions get restarted in this container, keep relaunching on an interval when we crash
export HOME=/home/ubuntu

# Trust the certificate and create an eclipse storage password file (avoid annoying UI prompts)
echo "ubuntu" > /home/ubuntu/.eclipsePass

until sudo -u ubuntu bash -c "cd /home/ubuntu; HOME=/home/ubuntu /keytool-trust.sh $RTC_HOSTNAME 9443"
do
	echo "Waiting for server to come up..."
	sleep 2
done

mv /home/ubuntu/.keystore /home/ubuntu/.jazzcerts
chown -R ubuntu:ubuntu /home/ubuntu/.eclipsePass

while true; do
	sudo -u ubuntu -E HOME=/home/ubuntu PWD=/home/ubuntu lxterminal &
	sudo -u ubuntu firefox https://$RTC_HOSTNAME:9443/ccm/web &
	sudo -u ubuntu -E HOME=/home/ubuntu /jazz/client/eclipse/eclipse -consoleLog -console 2001 -data /home/ubuntu/workspace -eclipse.password /home/ubuntu/.eclipsePass -vmargs -Xdebug -Xrunjdwp:transport=dt_socket,address=8099,server=y,suspend=n -Dorg.eclipse.swt.browser.UseWebKitGTK=true
	sleep 2
done
