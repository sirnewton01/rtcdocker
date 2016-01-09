#!/bin/bash

set -e

cd /server-patches
for zipfile in *.zip
do
	echo "Unzipping $zipfile"
	[ -f "$zipfile" ] && unzip $zipfile && rm -f $zipfile
done

for jarfile in *.jar
do
	echo "Overwriting $jarfile with patched version"
	if [ -f "$jarfile" ]; then
		match=`find /ccmserver -name "$jarfile" -print`
		if [ -z "$match" ]; then
			echo "Could not find $jarfile to replace" && exit 1
		else
			cp $jarfile $match
		fi
	fi
done


