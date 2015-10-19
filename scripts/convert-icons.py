#!/usr/bin/env python
import os
import sh
import sys

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
    def __init__(self, path, filters):
        self.dp = int(os.path.basename(path))
        self.path = path
        self.svgs = []
        all_svgs = self.find_svg_files(path)
        filtered_svgs = self.filter_filenames(all_svgs, filters)
        self.svgs = self.abspath(filtered_svgs)

    @staticmethod
    def find_svg_files(path):
        return [p for p in glob(os.path.join(path, "*.svg"))]

    @staticmethod
    def filter_filenames(all_svg_files, filters=None):
        relative_svg_files = []
        if filters:
            for filter in filters:
                if os.path.join(source_path, filter) in all_svg_files:
                    relative_svg_files.append(os.path.join(source_path, filter))
        else:
            relative_svg_files = all_svg_files
        return relative_svg_files

    @staticmethod
    def abspath(filenames):
        output = []
        for filename in filenames:
            output.append(os.path.abspath(filename))
        return output

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
        return output_file_path, noflip

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
                self._do_export("xxxhdpi", svg, "drawable")
            else:
                for density in DENSITIES.keys():
                    self._do_flop(density, self._do_export(density, svg, "drawable-" + density))
            print(u"\u2713 %s" % os.path.basename(svg))


def validate_filters(filter_set):
    for filter in filter_set:
        if not filter.endswith(".svg") or "/" in filter:
            print >> sys.stderr, 'Only svg file names allowed in arguments.'
            sys.exit(-1)
    return filter_set

if __name__ == "__main__":
    svg_filters = None
    if len(sys.argv) > 1:
        svg_filters = validate_filters(set(sys.argv[1:]))
    source_density_paths = glob(os.path.join(os.path.dirname(__file__), "../icon-svgs/*"))
    for source_path in source_density_paths:
        ib = ImagesBatch(source_path, svg_filters)
        ib.convert()
