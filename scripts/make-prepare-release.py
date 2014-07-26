#!/usr/bin/env python
"""
Script that helps stage from master to beta to stable to alternative appstores.

Does the following things in two steps:
Step 1: (e.g., --beta):
    - Move package from org.wikipedia to org.wikipedia<.package> if appropriate
    - If appropriate, move folders to accommodate new packages
    - Replace all instances of string 'org.wikipedia' to 'org.wikipedia<.package>' if appropriate
    - Setup app to use beta icon, if appropriate
    - Bump versionCode and versionName, if appropriate
    - Make a new commit on a new branch
    - Creates an annotated tag called 'releases/versionName'
Step 2: (e.g., --beta --push):
    - Pushes the git tag created in step 1 to the gerrit remote

--beta build Step 1 revs the version code by 1.
--prod build Step 1 does NOT rev the version code
--amazon build Step 1 does NOT rev the version code, either


To run
1) git checkout master
2) git pull
3) git reset --hard
4) cd scripts
5) python make-prepare-release.py --beta
6) note the branch and tag with
    git branch | grep '*'
    git describe
and then build the APK and test
7) python make-prepare-release.py --beta --push
8) Get signoff and verbiage and deploy the beta to Google Play under Wikipedia Beta (app, not "beta" subsection)

Now, wait a week. Now that it's time to deploy to Google Play do this:

1) git checkout <beta_branch_you_cut>^
Note that's setting you back to the commit right before the BETA build.
2) git reset --hard
3) if necessary, git cherry-pick and manually merge any painful stuff as necessary
4) cd scripts
5) python make-prepare-release.py --prod
6) note the branch and tag with
    git branch | grep '*'
    git describe
and then build the APK and test
7) python make-prepare-release.py  --prod --push
8) Get signoff and verbiage and deploy to Google Play under Wikipedia


For the Amazon Appstore, take the stable build and follow the same instructions:

1) git checkout <stable_branch_you_cut>
!!!NOTE THIS IS THE TIP OF THE STABLE, NOT THE COMMIT BEFORE IT!!!
2) git reset --hard
3) if necessary, git cherry-pick and manually merge any painful stuff as necessary (hopefully extremely uncommon)
4) cd scripts
5) python make-prepare-release.py --amazon
6) note the branch and tag with
    git branch | grep '*'
    git describe
and then build the APK and test
7) python make-prepare-release.py  --amazon --push
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
    return 'tagged_releases/' + get_release_name(target)


def git_tag(target):
    """
    Creates an annotated git tag for this release
    """
    sh.git.tag('-a', get_git_tag_name(target), '-m', target)


def push_git_tag(target):
    """
    Pushes the git tag to gerrit
    """
    tag_name = get_git_tag_name(target)
    print('pushing ' + tag_name)
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
    else:
        return data


def change_label(data, target, channel, package, uprev):
    """
    Utility function to replace app label with beta app name, if appropriate
    """
    if package == 'beta':
        return data.replace('@string/app_name', '@string/app_name_beta')
    else:
        return data


versionCode_regex = re.compile(r'android:versionCode="(\d+)"', re.MULTILINE)
versionName_regex = re.compile(r'android:versionName="([^"]+)"', re.MULTILINE)


def set_version(data, target, channel, package, uprev):
    """
    Utility function to set new versionCode and versionName, if appropriate
    """
    if uprev:
        version_code = int(versionCode_regex.search(data).groups()[0])
        data = versionCode_regex.sub(
            'android:versionCode="%d"' % (version_code + 1),
            data
        )

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

    transform_file(p(dir_path, 'AndroidManifest.xml'), target, channel, package, uprev, replace_packagenames,
                   set_version, set_channel, change_icon, change_label)


def make_release(target, channel, package, uprev):
    sh.git.checkout('-b', 'releases/%s' % get_release_name(target))
    transform_project('wikipedia', target, channel, package, uprev)
    transform_project('wikipedia-it', target, channel, package, uprev)
    sh.cd(PATH_PREFIX)
    sh.git.add('-u')
    sh.git.commit('-m', 'Make release %s for %s' % (get_release_name(target), channel))
    git_tag(target)


if __name__ == '__main__':
    reminder = 'Please build the APK and test. After that, run w/ --push flag for history, and release the tested APK.'
    parser = argparse.ArgumentParser()
    group = parser.add_mutually_exclusive_group()
    group.add_argument('--beta',
                       help='Step 1: Google Play Beta. git checkout master; git pull first! Revs versionCode.',
                       action='store_true')
    group.add_argument('--prod',
                       help='Step 1: Google Play stable. git checkout BETATAG^ first! Revs versionCode.',
                       action='store_true')
    group.add_argument('--releasesprod',
                       help='Step 1: releasesdot stable. git checkout PLAYTAG first! Does not rev versionCode.',
                       action='store_true')
    group.add_argument('--amazon',
                       help='Step 1: Amazon stable release. git checkout PLAYTAG first! Does not rev versionCode.',
                       action='store_true')
    group.add_argument('--channel',
                       help='Step 1: Alphabetic versionName&channel. Usually, git checkout PLAYTAG first. OEMs w/ Play')
    group.add_argument('--custompackage',
                       help='Step 1: Alphabetic versionName&channel&package; Don\'t rev versionCode. OEMs wout/ Play.')
    parser.add_argument('--push', help='Step 2: push git tag created in step 1 to gerrit remote.', action='store_true')
    args = parser.parse_args()
    if args.beta:
        if args.push:
            push_git_tag('beta')
        else:
            make_release('beta', 'Google Play Beta Channel', 'beta', True)
            print(reminder)
    elif args.prod:
        if args.push:
            push_git_tag('r')
        else:
            make_release('r', 'Google Play', '', True)
            print(reminder)
    elif args.releasesprod:
        if args.push:
            push_git_tag('releasesprod')
        else:
            make_release('releasesprod', 'Releases Stable Channel', '', False)
            print(reminder)
    elif args.amazon:
        if args.push:
            push_git_tag('amazon')
        else:
            make_release('amazon', 'Amazon Appstore', '', False)
            print(reminder)
    elif args.channel:
        if args.push:
            push_git_tag(args.channel)
        else:
            make_release(args.channel, args.channel, '', False)
            print(reminder)
    elif args.custompackage:
        if args.push:
            push_git_tag(args.custompackage)
        else:
            make_release(args.custompackage, args.custompackage, args.custompackage, False)
            print(reminder)
    else:
        print('Error. Please specify a target in --beta, --prod, or --amazon')
