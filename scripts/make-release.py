#!/usr/bin/env python
"""
Script that builds one or more release apks.

Does the following things in several steps:

Step 1: (e.g., --beta):
    - Runs the selected (clean) Gradle builds (e.g. beta, prod, custom)

Step 2: (e.g., --beta --push):
    - Creates an annotated tag called 'releases/versionName'
    - Pushes the git tag to origin for history
    - TODO (Not implemented yet): Uploads certain bits to releases.mediawiki.org: r, beta

To run
1) tell people on #wikimedia-mobile you're about to bump the version,
   so hold off on merging to main
2) git checkout main
3) git pull
4) git reset --hard
5) python scripts/make-release.py --prod
Note: the apk file locations are printed to stdout
6) manual step: test the apk(s): adb install -r <apk file>
7) python scripts/make-release.py --prod --push
8) compile release note of prod using
    git log --pretty=format:"%h | %cr | %s" --abbrev-commit --no-merges `git tag -l r/*|tail -1`..
9) Upload prod apk to Play Store and to releases.mediawiki.org

Requires the python module 'sh' and the environment variable ANDROID_HOME to run.
Ensure you have a clean working directory before running as well.

See also https://www.mediawiki.org/wiki/Wikimedia_Apps/Team/Release_process
"""
import argparse
import glob
import os
import re
import sh
import subprocess
import sys

PATH_PREFIX = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
GRADLEW = './gradlew'
VERSION_START = '2.7'


def p(*path_fragments):
    """
    Combine the path fragments with PATH_PREFIX as a base, and return
    the new full path
    """
    return os.path.join(PATH_PREFIX, *path_fragments)


def get_git_tag_name(target, version_name):
    """
    Returns name used for creating the tag
    """
    return target + '/' + version_name


def git_tag(target, version_name):
    """
    Creates an annotated git tag for this release
    """
    sh.git.tag('-a', get_git_tag_name(target, version_name), '-m', target)


def git_push_tag(target, version_name):
    """
    Pushes the git tag to origin
    """
    tag_name = get_git_tag_name(target, version_name)
    print('pushing tag ' + tag_name)
    sh.git.push('origin', tag_name)


def make_release(flavors, custom_channel):
    sh.cd(PATH_PREFIX)
    args = [GRADLEW,
            '-q',
            'clean',
            '-PcustomChannel=' + custom_channel]
    tasks = ['assemble{0}Release'.format(flavor.title()) for flavor in flavors]
    args += tasks
    subprocess.call(args)

def make_bundle(flavors, custom_channel):
    sh.cd(PATH_PREFIX)
    args = [GRADLEW,
            '-q',
            'clean',
            '-PcustomChannel=' + custom_channel]
    tasks = ['bundle{0}Release'.format(flavor.title()) for flavor in flavors]
    args += tasks
    subprocess.call(args)


def get_output_apk_file_name(flavor):
    return 'app/build/outputs/apk/' + flavor + '/release/app-' + flavor + '-release.apk'

def get_output_bundle_file_name(flavor):
    return 'app/build/outputs/bundle/' + flavor + 'Release/app-' + flavor + '-release.aab'


def get_android_home():
    android_home = os.environ['ANDROID_HOME']
    if android_home:
        return android_home
    else:
        sys.exit('$ANDROID_HOME not set')


def grep_from_build_file(property_name, regex):
    build_gradle_file_name = 'app/build.gradle'
    with open(build_gradle_file_name, "r") as build_file:
        for line in build_file:
            found = re.search(regex, line)
            if found:
                res = found.groups()[0]
                return res
    sys.exit("Could not find %s in %s" % (property_name, build_gradle_file_name))


def get_build_tools_version_from_build_file():
    return grep_from_build_file('buildToolsVersion', r'buildToolsVersion\s+\'(\S+)\'')


def get_version_code_from_build_file():
    return grep_from_build_file('versionCode', r'versionCode\s+(\S+)')


def get_version_name_from_apk(apk_file):
    aapt = '%s/build-tools/%s/aapt' % (get_android_home(), next(os.walk('%s/build-tools/' % get_android_home()))[1][0])
    process = subprocess.check_output([aapt, 'dump', 'badging', apk_file])
    found = re.search(r'versionName=\'(\S+)\'', process)
    if found:
        apk_version_name = found.groups()[0]
        return apk_version_name
    else:
        sys.exit("Could not get version name from apk " + apk_file)


def copy_apk(flavor, version_name):
    folder_path = 'releases'
    sh.mkdir("-p", folder_path)
    output_file = '%s/wikipedia-%s.apk' % (folder_path, version_name)
    sh.cp(get_output_apk_file_name(flavor), output_file)
    print ' apk: %s' % output_file

def copy_bundle(flavor, version_name):
    folder_path = 'releases'
    sh.mkdir("-p", folder_path)
    output_file = '%s/wikipedia-%s.aab' % (folder_path, version_name)
    sh.cp(get_output_bundle_file_name(flavor), output_file)
    print ' apk: %s' % output_file


def find_output_apk_for(label, version_code):
    folder_path = 'releases'
    file_pattern = '%s/wikipedia-%s.%s-%s-*.apk' % (folder_path, VERSION_START, version_code, label)
    apk_files = glob.glob(file_pattern)
    if len(apk_files) == 1:
        return apk_files[0]
    elif len(apk_files) == 0:
        sys.exit("Did not find apk files for %s" % file_pattern)
    else:
        sys.exit("Found too many(%d) files for %s" % (len(apk_files), file_pattern))


def main():
    parser = argparse.ArgumentParser()
    group = parser.add_mutually_exclusive_group()
    group.add_argument('--alpha',
                       help='Step 1: Alpha for testing.',
                       action='store_true')
    group.add_argument('--beta',
                       help='Step 1: Google Play Beta. git checkout BUMPTAG first!',
                       action='store_true')
    group.add_argument('--prod',
                       help='Step 1: Google Play stable.',
                       action='store_true')
    group.add_argument('--channel',
                       help='Step 1: Custom versionName&channel. OEMs w/ Play')
    group.add_argument('--app',
                       help='Step 1: Custom versionName&channel. OEMs wout/ Play.')
    parser.add_argument('--push', help='Step 2: create&push git tag to origin.',
                        action='store_true')
    args = parser.parse_args()
    custom_channel = 'ignore'
    if args.alpha:
        flavors = ['alpha']
        targets = flavors
    elif args.beta:
        flavors = ['beta']
        targets = flavors
    elif args.prod:
        flavors = ['prod']
        targets = ['r']
    elif args.channel:
        flavors = ['custom']
        targets = [args.channel]
        custom_channel = args.channel
    elif args.app:
        flavors = ['custom']
        targets = [args.app]
        custom_channel = args.app
    else:
        print('Error. Please specify --beta, --prod, etc.')
        sys.exit(-1)

    if args.push:
        if custom_channel is 'ignore':
            label = targets[0]
        else:
            label = custom_channel
        apk_file = find_output_apk_for(label, get_version_code_from_build_file())
        version_name = get_version_name_from_apk(apk_file)
        for target in targets:
            git_tag(target, version_name)
            git_push_tag(target, version_name)
    else:
        folder_path = 'releases'
        sh.mkdir("-p", folder_path)

        print('Building APK...')
        make_release(flavors, custom_channel)
        version_name = get_version_name_from_apk(get_output_apk_file_name(flavors[0]))
        print('Copying APK...')
        copy_apk(flavors[0], version_name)

        print('Building bundle...')
        make_bundle(flavors, custom_channel)
        print('Copying bundle...')
        copy_bundle(flavors[0], version_name)

        print('Please test the APK. After that, run w/ --push flag, and release the tested APK.')
        print('A useful command for collecting the release notes:')
        print('git log --pretty=format:"%h | %cr | %s" --abbrev-commit --no-merges ' +
              '`git tag --list ' + targets[0] + '/* | tail -1`..')


if __name__ == '__main__':
    main()
