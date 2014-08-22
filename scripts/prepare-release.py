#!/usr/bin/env python
"""
Script that helps stage from master to beta to stable to alternative appstores.

Does the following things in several steps:

Step 0: (--bump, git review, then --bump --push)
    - Do this from the tip of master
    - Increases the versionNumber


IMPORTANT NOTE!
If you need to cherry-pick changes to the release branch then make sure you have a local branch for the commit from
which to cherry-pick. For example, "git checkout -b" after you fetch someone else's changes from Gerrit.

Step 1: (e.g., --beta):
    - Move package from org.wikipedia to org.wikipedia<.package> if appropriate
    - If appropriate, move folders to accommodate new packages
    - Replace all instances of string 'org.wikipedia' to 'org.wikipedia<.package>' if appropriate
    - Setup app to use beta icon, if appropriate
    - Modify versionName
    - Make a new commit on a new branch
    - Create an annotated tag called 'releases/versionName'

Step 2: (e.g., --beta --push):
    - Pushes the git branch and tag created in step 1 to gerrit for history

To run
1) tell people on #wikimedia-mobile you're about to bump the version, so hold off on merging to master
2) git checkout master
3) git pull
4) git reset --hard
5) cd scripts
6) python prepare-release.py --bump
7) note the branch and tag with
       git branch | grep '*'
       git describe
8) git review
9) approve in Gerrit right away
10) python prepare-release.py --bump --push

1) git checkout <bump_branch_you_cut>
2) git reset --hard
3) cd scripts
4) python prepare-release.py --beta
5) note the branch and tag with
    git branch | grep '*'
    git describe
and then build the APK and test
6) python prepare-release.py --beta --push
7) Get signoff and verbiage and deploy the beta to Google Play under Wikipedia Beta (app, not "beta" subsection)

Now, wait until it's time to deploy to Google Play stable, then do this:

1) git checkout <bump_branch_you_cut>
2) git reset --hard
3) if necessary, git cherry-pick and manually merge any painful stuff as necessary
4) cd scripts
5) python prepare-release.py --prod
6) note the branch and tag with
    git branch | grep '*'
    git describe
and then build the APK and test
7) python prepare-release.py --prod --push
8) Get signoff and verbiage and deploy to Google Play under Wikipedia (not Wikipedia Beta!)


For the Amazon Appstore, take the stable build and follow the same instructions:

1) git checkout <bump_branch_you_cut>
2) git reset --hard
3) if necessary, git cherry-pick and manually merge any painful stuff as necessary (hopefully extremely uncommon)
4) cd scripts
5) python prepare-release.py --amazon
6) note the branch and tag with
    git branch | grep '*'
    git describe
and then build the APK and test
7) python prepare-release.py --amazon --push
8) Double check verbiage from Google Play, taking feature diffs into account from previous Amazon Appstore
 upload. Then go into the Amazon developer portal and upload the latest APK, ideally setting the introduction
 date of the APK for standard hours.

Requires the python module 'sh' to run. Ensure you have a clean working
directory before running as well.
"""
import sh
import os
import re
import time
import argparse
import sys

PATH_PREFIX = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))


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


def git_mv_dir(dir_path, package):
    """
    Performs git mv from main package to a <.package> subpackage, when called
    """
    # Need to do the move in two forms, since we can not
    # move a directory to be its own subdirectory in one step
    sh.git.mv(
        p(dir_path, 'src/main/java/org/wikipedia'),
        p(dir_path, 'src/main/java/org/' + package)
    )

    sh.mkdir('-p', p(dir_path, 'src/main/java/org/wikipedia'))

    sh.git.mv(
        p(dir_path, 'src/main/java/org/' + package),
        p(dir_path, 'src/main/java/org/wikipedia')
    )


def transform_file(file_path, target, channel, package, uprev, *funcs):
    """
    Transforms the file given in file_path by passing it
    serially through all the functions in *func and then
    writing it back out to file_path
    """
    f = open(file_path, 'r+')
    data = f.read()
    f.seek(0)
    for func in funcs:
        data = func(data, target, channel, package, uprev)
    f.write(data)
    f.close()
    print file_path


def replace_packagenames(data, target, channel, package, uprev):
    """
    Utility function to replace all non-beta package names
    with beta package names
    """
    if package:
        return data.replace('org.wikipedia', 'org.wikipedia.' + package)
    else:
        return data


def change_icon(data, target, channel, package, uprev):
    """
    Utility function to replace launcher icon with
    beta launcher icon, if appropriate
    """
    if package == 'beta':
        return data.replace("launcher", "launcher_beta")
    elif package == 'alpha':
        return data.replace("launcher", "launcher_alpha")
    else:
        return data


def change_label(data, target, channel, package, uprev):
    """
    Utility function to replace app label with beta app name, if appropriate
    """
    if package == 'beta':
        return data.replace('@string/app_name', '@string/app_name_beta')
    elif package == 'alpha':
        return data.replace('@string/app_name', '@string/app_name_alpha')
    else:
        return data


versionCode_regex = re.compile(r'android:versionCode="(\d+)"', re.MULTILINE)
versionName_regex = re.compile(r'android:versionName="([^"]+)"', re.MULTILINE)


def set_version_code(data, target, channel, package, uprev):
    """
    Utility function to set new versionCode, if appropriate
    """
    if uprev:
        version_code = int(versionCode_regex.search(data).groups()[0])
        data = versionCode_regex.sub(
            'android:versionCode="%d"' % (version_code + 1),
            data
        )
    return data


def set_version_name(data, target, channel, package, uprev):
    """
    Utility function to set new versionName, if appropriate
    """
    data = versionName_regex.sub(
        'android:versionName="%s"' % get_release_name(target),
        data
    )
    return data


versionChannel_regex = re.compile(r'<meta-data android:value="(.*)" android:name="@string/preference_channel">',
                                  re.MULTILINE)


def set_channel(data, target, channel, package, uprev):
    """
    Utility function to set new channel
    """
    if channel:
        data = versionChannel_regex.sub(
            '<meta-data android:value="%s" android:name="@string/preference_channel">' % channel,
            data
        )
        return data


def transform_project(dir_path, target, channel, package, uprev):
    """
    Performs all necessary transformations for a particular project
    """
    if package:
        git_mv_dir(dir_path, package)
        for root, dirs, files in os.walk(p(dir_path, 'src/main/java/org/wikipedia/' + package)):
            for file_name in files:
                file_path = os.path.join(root, file_name)
                transform_file(file_path, target, channel, package, uprev, replace_packagenames)

        for root, dirs, files in os.walk(p(dir_path, 'res')):
            for file_name in files:
                if file_name.endswith('.xml'):
                    file_path = os.path.join(root, file_name)
                    transform_file(file_path, target, channel, package, uprev, replace_packagenames)

    if dir_path.endswith('-it'):
        transform_file(p(dir_path, 'AndroidManifest.xml'), target, channel, package, uprev, replace_packagenames)
    else:
        transform_file(p(dir_path, 'AndroidManifest.xml'), target, channel, package, uprev, replace_packagenames,
                       set_version_name, set_channel, change_icon, change_label)


def transform_project_for_uprev(dir_path, target, channel, package, uprev):
    transform_file(p(dir_path, 'AndroidManifest.xml'), target, channel, package, uprev, set_version_code)


def make_release(target, channel, package, uprev):
    # other changes on a new branch
    sh.git.checkout('-b', 'releases/%s' % get_release_name(target))
    transform_project('wikipedia', target, channel, package, uprev)
    transform_project('wikipedia-it', target, channel, package, uprev)
    sh.cd(PATH_PREFIX)
    sh.git.add('-u')
    sh.git.commit('-m', 'Make release %s for %s' % (get_release_name(target), channel))
    git_tag(target)


def bump(target, channel, package, uprev):
    # uprevs are done on master
    sh.git.checkout('-b', 'bump-%s' % get_release_name(target))
    transform_project_for_uprev('wikipedia', target, channel, package, uprev)
    sh.cd(PATH_PREFIX)
    sh.git.add('-u')
    sh.git.commit('-m', 'Bump versionCode')
    git_tag(target)


def main():
    parser = argparse.ArgumentParser()
    group = parser.add_mutually_exclusive_group()
    group.add_argument('--bump',
                       help='Step 0: Uprev the versionCode. git checkout master; git pull first! Revs versionCode.',
                       action='store_true')
    group.add_argument('--beta',
                       help='Step 1: Google Play Beta. git checkout BUMPTAG first! Does not revs versionCode.',
                       action='store_true')
    group.add_argument('--alpha',
                       help='Do not use manually, only for the automated build script',
                       action='store_true')
    group.add_argument('--prod',
                       help='Step 1: Google Play stable. git checkout BUMPTAG first! Does not rev versionCode.',
                       action='store_true')
    group.add_argument('--releasesprod',
                       help='Step 1: releasesdot stable. git checkout BUMPTAG first! Does not rev versionCode.',
                       action='store_true')
    group.add_argument('--amazon',
                       help='Step 1: Amazon stable release. git checkout BUMPTAG first! Does not rev versionCode.',
                       action='store_true')
    group.add_argument('--channel',
                       help='Step 1: Alphabetic versionName&channel. Usually, git checkout BUMPTAG first. OEMs w/ Play')
    group.add_argument('--custompackage',
                       help='Step 1: Alphabetic versionName&channel&package; Don\'t rev versionCode. OEMs wout/ Play.')
    parser.add_argument('--push', help='Step 2: push git tag created in step 1 to gerrit remote.', action='store_true')
    args = parser.parse_args()
    # TODO: use something other than 'master' for the standalone branch and tag for bump?
    if args.bump:
        (target, channel, package, uprev) = ('master', 'master', '', True)
    elif args.beta:
        (target, channel, package, uprev) = ('beta', 'Google Play Beta Channel', 'beta', False)
    elif args.alpha:
        (target, channel, package, uprev) = ('alpha', 'alpha', 'alpha', False)
    elif args.prod:
        (target, channel, package, uprev) = ('r', 'Google Play', '', False)
    elif args.releasesprod:
        (target, channel, package, uprev) = ('releasesprod', 'Releases Stable Channel', '', False)
    elif args.amazon:
        (target, channel, package, uprev) = ('amazon', 'Amazon Appstore', '', False)
    elif args.channel:
        (target, channel, package, uprev) = (args.channel, args.channel, '', False)
    elif args.custompackage:
        (target, channel, package, uprev) = (args.custompackage, args.custompackage, args.custompackage, False)
    else:
        print('Error. Please specify --bump, --beta, --prod, --releasesprod, --amazon, --channel, --custompackage')
        sys.exit(-1)

    # TODO: use something other than 'master' for the standalone branch and tag for bump?
    if args.push:
        push_to_gerrit(target)
    elif args.bump:
        bump(target, channel, package, uprev)
        print('BUMP NOTICE!')
        print('BUMP NOTICE! Run git review with bumped version and +2 if appropriate,')
        print('BUMP NOTICE! Then re-run the script with --bump --push.')
        print('BUMP NOTICE!')
    else:
        make_release(target, channel, package, uprev)
        print('Please build the APK and test. After that, run w/ --push flag, and as needed release the tested APK.')
        print('A useful command for collecting the release notes:')
        print('git log --pretty=format:"%h %s" --abbrev-commit --no-merges <previous release tag>..')


if __name__ == '__main__':
    main()
