#!/usr/bin/env python
import sh
import os
import re
import time

PATH_PREFIX = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))


def p(*path_fragments):
    return os.path.join(PATH_PREFIX, *path_fragments)


def get_beta_name():
    return '2.0-beta-%s' % time.strftime('%Y-%m-%d')


def mv_dir(dir_path):
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
    f = open(file_path, 'r+')
    data = f.read()
    f.seek(0)
    for func in funcs:
        data = func(data)
    f.write(data)
    f.close()
    print file_path


def replace_packagenames(data):
    return data.replace('org.wikipedia', 'org.wikipedia.beta')


def change_icon(data):
    return data.replace("launcher_alpha", "launcher_beta")

versionCode_regex = re.compile(r'android:versionCode="(\d+)"', re.MULTILINE)
versionName_regex = re.compile(r'android:versionName="([^"]+)"', re.MULTILINE)


def set_version(data):
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
    mv_dir(dir_path)
    for root, dirs, files in os.walk(p(dir_path, 'src/main/java/org/wikipedia/beta')):
        for file_name in files:
            file_path = os.path.join(root, file_name)
            transform_file(file_path, replace_packagenames)

    for root, dirs, files in os.walk(p(dir_path, 'res')):
        for file_name in files:
            if file_name.endswith('.xml'):
                file_path = os.path.join(root, file_name)
                transform_file(file_path, replace_packagenames)

    transform_file(p(dir_path, 'AndroidManifest.xml'), replace_packagenames, set_version, change_icon)

if __name__ == '__main__':
    sh.git.checkout('-b', 'betas/%s' % get_beta_name())
    transform_project('wikipedia')
    transform_project('wikipedia-it')
    sh.cd(PATH_PREFIX)
    sh.git.add('-u')
    sh.git.commit('-m', 'Make release %s' % get_beta_name())
