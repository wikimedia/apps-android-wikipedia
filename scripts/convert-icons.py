import os
import sh

from glob import glob

DENSITIES = {
    "ldpi": 0.75,
    "mdpi": 1,
    "hdpi": 1.5,
    "xhdpi": 2,
    "xxhdpi": 3
}

OUTPUT_PATH_PREFIX = os.path.abspath(os.path.join(os.path.dirname(__file__), "../wikipedia/res/"))


class ImagesBatch(object):
    def __init__(self, path):
        self.dp = int(os.path.basename(path))
        self.path = path
        self.svgs = [os.path.abspath(p) for p in glob(os.path.join(path, "*.svg"))]

    def _do_export(self, density, input_path):
        file_name = os.path.basename(os.path.splitext(input_path)[0] + ".png")
        output_file_path = os.path.join(OUTPUT_PATH_PREFIX, "drawable-" + density, file_name)
        px = int(DENSITIES[density] * self.dp)
        sh.rsvg_convert(input_path, "-a", h=px, o=output_file_path)
        return output_file_path

    def _do_flop(self, density, input_path):
        folder_name = os.path.join(OUTPUT_PATH_PREFIX, "drawable-ldrtl-" + density)
        output_file_path = os.path.join(folder_name, os.path.basename(input_path))
        sh.mkdir("-p", folder_name)
        sh.convert(input_path, "-flop", output_file_path)

    def convert(self):
        for svg in self.svgs:
            for density in DENSITIES.keys():
                self._do_flop(density, self._do_export(density, svg))
            print u"\u2713 %s" % os.path.basename(svg)


if __name__ == "__main__":
    paths = glob(os.path.join(os.path.dirname(__file__), "../icon-svgs/*"))
    for path in paths:
        ib = ImagesBatch(path)
        ib.convert()
