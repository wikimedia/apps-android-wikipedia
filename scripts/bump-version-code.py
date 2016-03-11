#!/usr/bin/env python
"""
Creates a commit that increments the versionCode in the build.gradle file.
We usually run this before releasing a new beta version to the store.

Does the following things:
Step 1: (run without arguments)
    - Bump versionCode
    - Make a new commit

After this run 'git review' and + the commit.

Requires the Python module 'sh' to run.
"""
import sh
import os
import re
import sys

VERSION_CODE_REGEX = r'(?P<key>versionCode) (?P<value>\d+)'

script_dir = sys.path[0]
parent_dir = os.path.join(script_dir, os.pardir)
path_prefix = os.path.abspath(parent_dir)

version_code_pattern = re.compile(VERSION_CODE_REGEX, re.MULTILINE)


def set_version_code(data):
    """
    Utility function to set new versionCode
    """
    match = version_code_pattern.search(data)
    if not match:
        raise ValueError('Version code not found')
    version_code = int(match.group('value'))
    next_version_code = '\g<key> {}'.format(version_code + 1)
    return version_code_pattern.sub(next_version_code, data)


def transform_file(file_path, *funcs):
    """
    Transforms the file given in file_path by passing it
    serially through all the functions in *func and then
    writing it back out to file_path
    """
    with open(file_path, 'r+') as f:
        data = f.read()
        f.seek(0)
        for func in funcs:
            data = func(data)
        f.write(data)
        print(file_path)


def bump(file_path):
    transform_file(file_path, set_version_code)
    sh.cd(path_prefix)
    sh.git.add('-u', file_path)
    sh.git.commit('-m', 'Bump versionCode')


if __name__ == '__main__':
    bump('app/build.gradle')
    print('BUMP NOTICE! Run git review with bumped version and +2 if appropriate,')
