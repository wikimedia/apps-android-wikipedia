#!/bin/bash

for F in icon-sources/*;
do
    echo "Converting $F"
    IMGNAME=`basename $F .svg`
    convert -resize 32x32 -background none "$F" "res/drawable-mdpi/$IMGNAME.png"
    convert -resize 48x48 -background none "$F" "res/drawable-hdpi/$IMGNAME.png"
    convert -resize 72x72 -background none "$F" "res/drawable-xhdpi/$IMGNAME.png"
done
