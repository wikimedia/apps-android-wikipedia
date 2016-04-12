#!/usr/bin/env python

import sys
import os
import re
import glob
import inspect
import sh

TRACE = False

DENSITIES = {'mdpi': 1, 'hdpi': 1.5, 'xhdpi': 2, 'xxhdpi': 3, 'xxxhdpi': 4}

OUTPUT_PATH_PREFIX = os.path.abspath(os.path.join(os.path.dirname(__file__),
                                                  '../app/src/main/res/'))


def trace_value_of(variable_name):
    """Format and output current value of named local variable."""
    if not TRACE:
        return
    value = inspect.currentframe().f_back.f_locals[variable_name]
    value_type = type(value)
    if value_type in set([int, str]):
        print('{}: {}'.format(variable_name, value))
    elif value_type in set([list, set]) and value:
        print(variable_name + ':')
        print('\n'.join('- ' + x for x in value))


def validate_filter_argument(filter_argument):
    if not filter_argument.endswith('.svg') or os.sep in filter_argument:
        raise SystemExit('Only svg file names allowed in arguments.')
    return filter_argument


def export(dp, density, filepath, drawable):
    noflip = '.nonspecific.' in filepath or filepath.endswith('.noflip.svg')
    filename = os.path.basename(filepath)
    new_filename = filename[:filename.index('.')] + '.png'
    folder_path = os.path.join(OUTPUT_PATH_PREFIX, drawable)
    sh.mkdir('-p', folder_path)
    output_file_path = os.path.join(folder_path, new_filename)
    output_precrush_path = output_file_path + '_'
    px = int(DENSITIES[density] * dp)
    sh.rsvg_convert(filepath, '-a', h=px, o=output_precrush_path)
    sh.pngcrush('-q', '-reduce', output_precrush_path, output_file_path)
    sh.rm(output_precrush_path)
    return output_file_path, noflip


def flop(density, (filepath, noflip)):
    if noflip:
        return
    folder_name = os.path.join(OUTPUT_PATH_PREFIX, 'drawable-ldrtl-' + density)
    output_file_path = os.path.join(folder_name, os.path.basename(filepath))
    sh.mkdir('-p', folder_name)
    sh.convert(filepath, '-flop', output_file_path)


def convert(icon_path, svg_filters):
    print('\n* icon_path: {}'.format(icon_path))

    dp = int(os.path.basename(icon_path))
    trace_value_of('dp')

    svg_glob = glob.glob(os.path.join(icon_path, '*.svg'))
    svg_files = ([x for x in svg_glob if os.path.basename(x) in svg_filters]
                 if svg_filters else svg_glob)

    if not svg_files:
        return
    print('converted:')
    for svg_file in svg_files:
        if '.nonspecific.' in svg_file:
            export(dp, 'xxxhdpi', svg_file, 'drawable')
        else:
            for density in DENSITIES:
                flop(density,
                     export(dp, density, svg_file, 'drawable-' + density))
        print('+ {}'.format(svg_file))


def is_a_size(name):
    return re.match(r'[1-9]\d*', name)


def main():
    executed_via = os.path.abspath(__file__)
    trace_value_of('executed_via')

    script_dir = os.path.dirname(executed_via)
    trace_value_of('script_dir')

    icons_dir = os.path.abspath(
        os.path.join(script_dir, os.pardir, 'icon-svgs'))
    trace_value_of('icons_dir')

    svg_filters = set(validate_filter_argument(x) for x in sys.argv[1:])
    trace_value_of('svg_filters')

    source_densities = list()
    for name in os.listdir(icons_dir):
        if not is_a_size(name):
            continue
        icon_path = os.path.join(icons_dir, name)
        if os.path.isdir(icon_path):
            source_densities.append(icon_path)
    trace_value_of('source_densities')

    for icon_path in source_densities:
        convert(icon_path, svg_filters)


if __name__ == '__main__':
    main()
