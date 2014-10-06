#!/usr/bin/env python
"""
Script that builds one or more release apks.

Does the following things in several steps:

Step 1: (e.g., --beta):
    - Runs the selected (clean) Gradle builds (e.g. beta, amazon, or prod and releasesprod combined)

Step 2: (e.g., --beta --push):
    - Create an annotated tag called 'releases/versionName'
    - Pushes the git branch and tag created in step 1 to gerrit for history
    - Upload certain bits to releases.mediawiki.org: releasesprod, beta

To run
1) tell people on #wikimedia-mobile you're about to bump the version, so hold off on merging to master
2) git checkout master
3) git pull
4) git reset --hard
5) python scripts/make-release.py --prod --releasesprod
6) manual step: test the apks: adb install -r <apk file>
7) python scripts/make-release.py --prod --releasesprod --push
8) compile release note using
    git log --graph --date=short `git tag -l beta/*|tail -1`..
9) Upload apk to store

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
    Returns name release, based on target (in release name) and current date
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
    Pushes the git branch and tag to gerrit
    """
    release_name = get_release_name(target)
    print('pushing branch ' + release_name)
    sh.git.push('gerrit', 'HEAD:refs/heads/releases/%s' % release_name)
    # don't need to do this?
    # sh.git.push('gerrit', 'HEAD:refs/for/releases/%s' % release_name)

    tag_name = get_git_tag_name(target)
    print('pushing tag ' + tag_name)
    sh.git.push('gerrit', tag_name)


def make_release(flavors):
    sh.cd(PATH_PREFIX)
    # ./gradlew -q assembleDevDebug
    args = [GRADLEW, '-q', 'clean']
    tasks = ['assemble{0}Release'.format(i.title()) for i in flavors]
    args += tasks
    subprocess.call(args)


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
        print('Please build the APK and test. After that, run w/ --push flag, and as needed release the tested APK.')
        print('A useful command for collecting the release notes:')
        print('git log --pretty=format:"%h %s" --abbrev-commit --no-merges <previous release tag>..')


if __name__ == '__main__':
    main()
