#!/usr/bin/env python
"""
Creates a commit that increments the versionCode in the build.gradle file.
We usually run this before releasing a new beta version to the store.

Does the following things:
Step 1: (run without arguments)
    - Bump versionCode
    - Make a new commit

Cross-platform compatible - works on Windows, macOS, and Linux.
"""
import subprocess
import os
import re
import sys

VERSION_CODE_BRANCH = 'bumpVersionCode'
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
    next_version_code = r'\g<key> {}'.format(version_code + 1)
    return version_code_pattern.sub(next_version_code, data)


def run_git_command(*args, cwd=None):
    """
    Run a git command with the given arguments in a cross-platform way.
    """
    cmd = ['git'] + list(args)
    try:
        result = subprocess.run(cmd, cwd=cwd, check=True, capture_output=True, text=True)
        return result.stdout.strip()
    except subprocess.CalledProcessError as e:
        raise RuntimeError(f"Git command failed: {' '.join(cmd)}\nError: {e.stderr}")


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
        f.truncate()  # Ensure file is properly truncated if new content is shorter
        print(file_path)


def bump(file_path):
    transform_file(file_path, set_version_code)

    # Change to the project directory
    os.chdir(path_prefix)

    # Checkout main branch
    run_git_command('checkout', 'main')

    # Try to delete existing branch
    try:
        run_git_command('branch', '-D', VERSION_CODE_BRANCH)
    except RuntimeError:
        print('Branch not deleted (safe to ignore).')

    # Create and checkout new branch
    run_git_command('checkout', '-b', VERSION_CODE_BRANCH)

    # Add the modified file
    run_git_command('add', '-u', file_path)

    # Commit the changes
    run_git_command('commit', '-m', 'Bump versionCode.')

    # Push the branch
    run_git_command('push', '--set-upstream', 'origin', VERSION_CODE_BRANCH)

    # Switch back to main
    run_git_command('checkout', 'main')


if __name__ == '__main__':
    bump('app/build.gradle')
    print('BUMP NOTICE! Merge the new `' + VERSION_CODE_BRANCH + '` into Main.')
