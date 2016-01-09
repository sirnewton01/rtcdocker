#!/bin/sh

#set -e

echo "Upgrading RTC..."

# Copy any derby databases over to the new server before doing the upgrade
pushd /ccmserver-upgrade/server
find . -name "derby" -prune -exec rm -rf /ccmserver-upgrade/server/{} \;
pushd /ccmserver
find . -name "derby" -exec cp -r {} /ccmserver-upgrade/{} \;
popd

# Perform the upgrade to both JTS and CCM
upgrade/jts/jts_upgrade.sh -oldJTSHome /ccmserver/server/conf -noPrompt -noStepPrompt -noVerify -noEditor
upgrade/ccm/ccm_upgrade.sh -oldApplicationHome /ccmserver/server/conf -noPrompt -noStepPrompt -noVerify -noEditor
popd

# Clean up the old server and move the new one into place
rm -rf /ccmserver
mv /ccmserver-upgrade /ccmserver
mv /install.sh /install-backup.sh

echo "Server upgrade is complete."
