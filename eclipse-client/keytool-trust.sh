#!/bin/bash

# Original version of this script can be found here: https://gist.github.com/ssbarnea/3164098

REMHOST=$1
REMPORT=${2:-443}

KEYSTORE_PASS=changeit
KEYTOOL=keytool

export PATH=$PATH:/jazz/client/eclipse/jdk/bin

# FYI: the default keystore is located in ~/.keystore

if [ -z "$REMHOST" ]
    then
    echo "ERROR: Please specify the server name to import the certificatin from, eventually followed by the port number, if other than 443."
    exit 1
    fi

set -e

rm -f $REMHOST.pem

if openssl s_client -connect $REMHOST:$REMPORT 1>/tmp/keytool_stdout 2>/tmp/output </dev/null
        then
        :
        else
        cat /tmp/keytool_stdout
        cat /tmp/output
        exit 1
        fi

if sed -ne '/-BEGIN CERTIFICATE-/,/-END CERTIFICATE-/p' </tmp/keytool_stdout > $REMHOST.pem
        then
        :
        else
        echo "ERROR: Unable to extract the certificate from $REMHOST:$REMPORT ($?)"
        cat /tmp/output
        fi

if $KEYTOOL -list -storepass ${KEYSTORE_PASS} -alias $REMHOST >/dev/null
    then
    echo "Key of $REMHOST already found, skipping it."
    else
    $KEYTOOL -import -trustcacerts -noprompt -storepass ${KEYSTORE_PASS} -alias $REMHOST -file $REMHOST.pem
    fi
