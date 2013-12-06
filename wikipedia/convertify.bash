#!/bin/bash

for F in icon-sources/*;
do
    echo "Converting $F"
    IMGNAME=`basename $F .svg`
    rsvg-convert -w 32 -h 32 -o "res/drawable-mdpi/$IMGNAME.png" "$F"
    rsvg-convert -w 48 -h 48 -o "res/drawable-hdpi/$IMGNAME.png" "$F"
    rsvg-convert -w 72 -h 72 -o "res/drawable-xhdpi/$IMGNAME.png" "$F"
    rsvg-convert -w 96 -h 96 -o "res/drawable-xxhdpi/$IMGNAME.png" "$F"
done
