#!/usr/bin/env python
import os
import sh

from glob import glob

DENSITIES = {
    "mdpi": 1,
    "hdpi": 1.5,
    "xhdpi": 2,
    "xxhdpi": 3,
    "xxxhdpi": 4
}

OUTPUT_PATH_PREFIX = os.path.abspath(os.path.join(os.path.dirname(__file__), "../app/src/main/res/"))


class ImagesBatch(object):
    def __init__(self, path):
        self.dp = int(os.path.basename(path))
        self.path = path
        self.svgs = [os.path.abspath(p) for p in glob(os.path.join(path, "*.svg"))]

    def _do_export(self, density, input_path, drawable):
        nonspecific = ".nonspecific." in input_path
        noflip = nonspecific or input_path.endswith(".noflip.svg")
        file_name = os.path.basename(os.path.splitext(input_path)[0].split(".")[0] + ".png")
        folder_path = os.path.join(OUTPUT_PATH_PREFIX, drawable)
        sh.mkdir("-p", folder_path)
        output_file_path = os.path.join(folder_path, file_name)
        output_precrush_path = output_file_path + "_"
        px = int(DENSITIES[density] * self.dp)
        sh.rsvg_convert(input_path, "-a", h=px, o=output_precrush_path)
        sh.pngcrush("-q", "-reduce", output_precrush_path, output_file_path)
        sh.rm(output_precrush_path)
        return (output_file_path, noflip)

    def _do_flop(self, density, (input_path, noflip)):
        if noflip:
            return
        folder_name = os.path.join(OUTPUT_PATH_PREFIX, "drawable-ldrtl-" + density)
        output_file_path = os.path.join(folder_name, os.path.basename(input_path))
        sh.mkdir("-p", folder_name)
        sh.convert(input_path, "-flop", output_file_path)

    def convert(self):
        for svg in self.svgs:
            if ".nonspecific." in svg:
                self._do_export("xxhdpi", svg, "drawable")
            else:
                for density in DENSITIES.keys():
                    # For xxxhdpi only generate launcher icons
                    if not (density == 'xxxhdpi' and 'launcher' not in svg):
                        self._do_flop(density, self._do_export(density, svg, "drawable-" + density))
            print(u"\u2713 %s" % os.path.basename(svg))


if __name__ == "__main__":
    paths = glob(os.path.join(os.path.dirname(__file__), "../icon-svgs/*"))
    for path in paths:
        ib = ImagesBatch(path)
        ib.convert()
