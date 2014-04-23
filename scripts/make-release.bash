#!/bin/bash

BASE_PATH="`dirname $0`/.."
ANDROID_MANIFEST_PATH="$BASE_PATH/wikipedia/AndroidManifest.xml"

# Figure out current Android versionCode
ANDROID_CUR_VERSION_CODE=`grep versionCode $ANDROID_MANIFEST_PATH | sed 's/[^0-9]//g'`

# Figure out new Android versionCode and versionName
ANDROID_NEW_VERSION_CODE=$(($ANDROID_CUR_VERSION_CODE + 1))
ANDROID_NEW_VERSION_NAME="2.0-alpha-`TZ="UTC" date "+%Y-%m-%d"`"

# Save where we are now
CUR_HEAD=`git rev-parse --abbrev-ref HEAD`

# Update to latest gerrit master and check out a new branch
git fetch gerrit
git checkout -b releases/$ANDROID_NEW_VERSION_NAME gerrit/master 

# Modify AndroidManifest.xml to have new versionCode and name
cat $ANDROID_MANIFEST_PATH \
    | sed "s/android:versionCode=.*$/android:versionCode=\"$ANDROID_NEW_VERSION_CODE\"/" \
    | sed "s/android:versionName=.*$/android:versionName=\"$ANDROID_NEW_VERSION_NAME\">/" \
    | tee $ANDROID_MANIFEST_PATH

# Commit changes and review
git add $ANDROID_MANIFEST_PATH
git commit -m "Bump version for release $ANDROID_NEW_VERSION_NAME"

# Read key info
read -p "Enter path to keystore file: " KEYPATH
read -s -p "Enter key passphrase: " KEYPASS

# Build the signed package
mvn package -Psign -DKEYPATH="$KEYPATH" -DKEYPASS="$KEYPASS"
if [ "m$?" == "m1" ]; then
    echo "Package failed!"
    exit 1
fi

# Move apk to releases folder
mkdir -p "$BASE_PATH/releases"
mv "$BASE_PATH/wikipedia/target/wikipedia.apk" "$BASE_PATH/releases/wikipedia-$ANDROID_NEW_VERSION_NAME.apk"

# Tag and push to gerrit for review
git tag -a "releases/$ANDROID_NEW_VERSION_NAME" -m "Tag for release $ANDROID_NEW_VERSION_NAME"
git push gerrit HEAD:refs/for/master

# Remove branch and go back to old location
git checkout $CUR_HEAD
git branch -D releases/$ANDROID_NEW_VERSION_NAME
