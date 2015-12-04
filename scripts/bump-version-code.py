#!/usr/bin/env python
"""
Script which creates a commit that increments the versionCode
in the build.gradle file. We usually run this before releasing
a new beta version to the store.

Does the following things:
Step 1: (run without arguments):
    - Bump versionCode
    - Make a new commit

After this run 'git review' and + the commit.

Requires the python module 'sh' to run.
"""
import sh
import os
import re

PATH_PREFIX = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
versionCode_regex = re.compile(r'versionCode (\d+)', re.MULTILINE)


def set_version_code(data):
    """
    Utility function to set new versionCode
    """
    version_code = int(versionCode_regex.search(data).groups()[0])
    data = versionCode_regex.sub(
        'versionCode %d' % (version_code + 1),
        data
    )
    return data


def transform_file(file_path, *funcs):
    """
    Transforms the file given in file_path by passing it
    serially through all the functions in *func and then
    writing it back out to file_path
    """
    f = open(file_path, 'r+')
    data = f.read()
    f.seek(0)
    for func in funcs:
        data = func(data)
    f.write(data)
    f.close()
    print(file_path)


def bump(file_path):
    transform_file(file_path, set_version_code)
    sh.cd(PATH_PREFIX)
    sh.git.add('-u', file_path)
    sh.git.commit('-m', 'Bump versionCode')


if __name__ == '__main__':
    bump('app/build.gradle')
    print('BUMP NOTICE! Run git review with bumped version and +2 if appropriate,')
