#!/usr/bin/env python
"""
Script that builds one or more release apks.

Does the following things in several steps:

Step 1: (e.g., --beta):
    - Runs the selected (clean) Gradle builds (e.g. beta, amazon, or prod and releasesprod combined)

Step 2: (e.g., --beta --push):
    - Creates an annotated tag called 'releases/versionName'
    - Pushes the git tag to gerrit for history
    - TODO (Not implemented yet): Uploads certain bits to releases.mediawiki.org: releasesprod, beta

To run
1) tell people on #wikimedia-mobile you're about to bump the version, so hold off on merging to master
2) git checkout master
3) git pull
4) git reset --hard
5) python scripts/make-release.py --prod
Note: the apk file locations are printed to stdout
6) manual step: test the apk(s): adb install -r <apk file>
7) python scripts/make-release.py --prod --push
8) compile release note of prod using (replace "r/*" with "beta/*" or "amazon/*")
    git log --pretty=format:"%h | %cr | %s" --abbrev-commit --no-merges `git tag -l r/*|tail -1`..
9) Upload prod apk to store, releasesprod apk to releases.mediawiki.org

Requires the python module 'sh' to run. Ensure you have a clean working
directory before running as well.
"""
import sh
import subprocess
import os
import time
import argparse
import sys

PATH_PREFIX = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
GRADLEW = './gradlew'


def p(*path_fragments):
    """
    Combine the path fragments with PATH_PREFIX as a base, and return
    the new full path
    """
    return os.path.join(PATH_PREFIX, *path_fragments)


def get_release_name(target):
    """
    Returns name release, based on target (in release name) and current date.
    This should be kept in sync with the versionNames of the various flavors
    in build.gradle.
    """
    return '2.0-%s-%s' % (target, time.strftime('%Y-%m-%d'))


def get_git_tag_name(target):
    """
    Returns name used for creating the tag
    """
    return target + '/' + get_release_name(target)


def git_tag(target):
    """
    Creates an annotated git tag for this release
    """
    sh.git.tag('-a', get_git_tag_name(target), '-m', target)


def push_to_gerrit(target):
    """
    Pushes the git tag to gerrit
    """
    tag_name = get_git_tag_name(target)
    print('pushing tag ' + tag_name)
    sh.git.push('gerrit', tag_name)


def make_release(flavors):
    sh.cd(PATH_PREFIX)
    # ./gradlew -q assembleDevDebug
    args = [GRADLEW, '-q', 'clean']
    tasks = ['assemble{0}Release'.format(flavor.title()) for flavor in flavors]
    args += tasks
    subprocess.call(args)


def copy_apk(flavor, target):
    folder_path = 'wikipedia/build/outputs/release'
    sh.mkdir("-p", folder_path)
    output_file = '%s/wikipedia-%s.apk' % (folder_path, get_release_name(target))
    sh.cp('wikipedia/build/outputs/apk/wikipedia-%s-release.apk' % flavor, output_file)
    print ' apk: %s' % output_file


def main():
    parser = argparse.ArgumentParser()
    group = parser.add_mutually_exclusive_group()
    group.add_argument('--beta',
                       help='Step 1: Google Play Beta. git checkout BUMPTAG first!',
                       action='store_true')
    # group.add_argument('--alpha',
    #                    help='Do not use manually, only for the automated build script',
    #                    action='store_true')
    group.add_argument('--prod',
                       help='Step 1: Google Play stable. git checkout BUMPTAG first!',
                       action='store_true')
    # group.add_argument('--releasesprod',
    #                    help='Step 1: releasesdot stable. git checkout BUMPTAG first!',
    #                    action='store_true')
    group.add_argument('--amazon',
                       help='Step 1: Amazon stable release. git checkout BUMPTAG first!',
                       action='store_true')
    # group.add_argument('--channel',
    #                    help='Step 1: Alphabetic versionName&channel. '
    #                         'Usually, git checkout BUMPTAG first. OEMs w/ Play')
    # group.add_argument('--custompackage',
    #                    help='Step 1: Alphabetic versionName&channel&package; OEMs wout/ Play.')
    parser.add_argument('--push', help='Step 2: push git tag created in step 1 to gerrit remote.',
                        action='store_true')
    args = parser.parse_args()
    if args.beta:
        flavors = ['beta']
        targets = flavors
    elif args.prod:
        flavors = ['prod', 'releasesprod']
        targets = ['r']
    elif args.amazon:
        flavors = ['amazon']
        targets = flavors
    # elif args.channel:
    #     flavors = [args.channel]
    #     targets = flavors
    else:
        print('Error. Please specify --beta, --prod, or --amazon')
        sys.exit(-1)

    if args.push:
        for target in targets:
            git_tag(target)
            push_to_gerrit(target)
    else:
        make_release(flavors)
        copy_apk(flavors[0], targets[0])
        if flavors[0] == 'prod':
            copy_apk(flavors[1], flavors[1])
        print('Please test the APK. After that, run w/ --push flag, and as needed release the tested APK.')
        print('A useful command for collecting the release notes:')
        print('git log --pretty=format:"%h | %cr | %s" --abbrev-commit --no-merges '
              + '`git tag --list ' + targets[0] + '/* | tail -1`..')


if __name__ == '__main__':
    main()
