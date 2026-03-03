#!/usr/bin/env bash

if [[ $# -eq 0 ]]
then
    echo "Which version of GMA are you packaging?"
    echo "Usage: $0 versionNum rootDir1 rootDir2 ..."
elif [[ $# -eq 1 ]]
then
    echo "Packaging GMA version $1 as a dmg."
    echo "Which directory or directories do you want to search in?"
    echo "Usage: $0 $1 rootDir1 rootDir2 ..."
else
    echo "Packaging GeoMapApp version $1 as a disk image for Mac users."
    read -p "If you wish to sign the disk image(s), put the name of the keychain profile you wish to use here: " -r
    keychain_profile=$REPLY
    for root_dir in "${@:2}"
    do
	gma_loc=$(find ${root_dir} -name "GeoMapApp.app")
	if [ -z "${gma_loc}" ]
	then
	    echo "Could not find GeoMapApp anywhere in ${root_dir}"
	else
	    executable=$(find ${gma_loc} -name "GeoMapApp")
	    src_dir=$(dirname ${gma_loc})
	    echo "Found GeoMapApp in ${src_dir}."
	    arch_full=$(file -b "${executable}")
	    arch_short="Unknown"
	    if [[ $arch_full == *"x86"* ]]
	    then
		arch_short="Intel"
	    else
		arch_short="Silicon"
	    fi
	    filename="GeoMapApp-$1-${arch_short}.dmg"
	    resource_path="src/main/resources/org/geomapapp/resources"
	    dmg_icon_path="${resource_path}/icons/GMA_dmg_icon.icns"
	    bg_path="dmg_resources/GeoMapApp-background.tiff"
	    echo "Creating $filename"
	    create-dmg --volname GeoMapApp --volicon ${dmg_icon_path} --background ${bg_path} --window-size 605 350 --icon GeoMapApp.app 100 60 --app-drop-link 510 60 ${filename} ${src_dir}
	    if [[ ! -z "${keychain_profile}" ]]
	    then
		echo "Now signing ${filename} as ${keychain_profile}."
		xcrun notarytool submit -p ${keychain_profile} ${filename} --wait
		xcrun stapler staple ${filename}
	    else
		echo "Not signing ${filename}. Make sure you sign it separately if you intend to distribute it publicly."
	    fi
	fi
    done
fi

