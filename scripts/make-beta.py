#!/usr/bin/env python
"""
Script that helps move the app from org.wikipedia to org.wikipedia.beta

Does the following things in two steps:
Step 1: (run without arguments):
    - Creates an annotated tag called 'releases/versionName'
    - Move package from org.wikipedia to org.wikipedia.beta
    - Move folders to accommodate new packages
    - Replace all instances of string 'org.wikipedia' to 'org.wikipedia.beta'
    - Setup app to use beta icon
    - Bump versionCode and versionName
    - Make a new commit on a new branch
Step 2: (run with --push argument):
    - Pushes the git tag created in step 1 to the gerrit remote

Requires the python module 'sh' to run. Ensure you have a clean working
directory before running as well.
"""
import sh
import os
import re
import time
import argparse

PATH_PREFIX = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))


def p(*path_fragments):
    """
    Combine the path fragments with PATH_PREFIX as a base, and return
    the new full path
    """
    return os.path.join(PATH_PREFIX, *path_fragments)


def get_beta_name():
    """
    Returns name used for beta naming, based on current date
    """
    return '2.0-beta-%s' % time.strftime('%Y-%m-%d')


def get_git_tag_name():
    """
    Returns name used for creating the tag
    """
    return 'releases/' + get_beta_name()


def git_tag():
    """
    Creates an annotated git tag for this release
    """
    sh.git.tag('-a', get_git_tag_name(), '-m', 'beta')


def push_git_tag():
    """
    Pushes the git tag to gerrit
    """
    print('pushing ' + get_git_tag_name())
    sh.git.push('gerrit', get_git_tag_name())


def git_mv_dir(dir_path):
    """
    Performs git mv from main package to a .beta subpackage
    """
    # Need to do the move in two forms, since we can not
    # move a directory to be its own subdirectory in one step
    sh.git.mv(
        p(dir_path, 'src/main/java/org/wikipedia'),
        p(dir_path, 'src/main/java/org/beta')
    )

    sh.mkdir('-p', p(dir_path, 'src/main/java/org/wikipedia'))

    sh.git.mv(
        p(dir_path, 'src/main/java/org/beta'),
        p(dir_path, 'src/main/java/org/wikipedia')
    )


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
    print file_path


def replace_packagenames(data):
    """
    Utility function to replace all non-beta package names
    with beta package names
    """
    return data.replace('org.wikipedia', 'org.wikipedia.beta')


def change_icon(data):
    """
    Utility function to replace launcher icon with
    beta launcher icon
    """
    return data.replace("launcher", "launcher_beta")


def change_label(data):
    """
    Utility function to replace app label with beta app name
    """
    return data.replace('@string/app_name', '@string/app_name_beta')


versionCode_regex = re.compile(r'android:versionCode="(\d+)"', re.MULTILINE)
versionName_regex = re.compile(r'android:versionName="([^"]+)"', re.MULTILINE)


def set_version(data):
    """
    Utility function to set new versionCode and versionName
    """
    new_version_name = get_beta_name()
    version_code = int(versionCode_regex.search(data).groups()[0])

    data = versionCode_regex.sub(
        'android:versionCode="%d"' % (version_code + 1),
        data
    )
    data = versionName_regex.sub(
        'android:versionName="%s"' % new_version_name,
        data
    )
    return data


def transform_project(dir_path):
    """
    Performs all necessary transformations for a particular project
    """
    git_mv_dir(dir_path)
    for root, dirs, files in os.walk(p(dir_path, 'src/main/java/org/wikipedia/beta')):
        for file_name in files:
            file_path = os.path.join(root, file_name)
            transform_file(file_path, replace_packagenames)

    for root, dirs, files in os.walk(p(dir_path, 'res')):
        for file_name in files:
            if file_name.endswith('.xml'):
                file_path = os.path.join(root, file_name)
                transform_file(file_path, replace_packagenames)

    transform_file(p(dir_path, 'AndroidManifest.xml'), replace_packagenames, set_version, change_icon, change_label)


def make_release():
    git_tag()
    sh.git.checkout('-b', 'betas/%s' % get_beta_name())
    transform_project('wikipedia')
    transform_project('wikipedia-it')
    sh.cd(PATH_PREFIX)
    sh.git.add('-u')
    sh.git.commit('-m', 'Make release %s' % get_beta_name())


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--push', help='step 2: push git tag created in step 1 to gerrit remote', action='store_true')
    args = parser.parse_args()
    if args.push:
        # step 2:
        push_git_tag()
    else:
        # step 1:
        make_release()
