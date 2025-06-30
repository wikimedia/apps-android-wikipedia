#!/usr/bin/env python
"""
Script that builds one or more release apks.

Does the following things in several steps:

Step 1: (e.g., --beta):
    - Runs the selected (clean) Gradle builds (e.g. beta, prod, custom)

Step 2: (e.g., --beta --push):
    - Creates an annotat    parser.add_argument('--bundle',
                       help='Build a bundle (AAB) in addition to APK.',
                       action='store_true')
    group.add_argument('--channel',
                       help='Step 1: Custom versionName&channel. OEMs w/ Play')
    group.add_argument('--app',
                       help='Step 1: Custom versionName&channel. OEMs wout/ Play.')
    parser.add_argument('--push', help='Step 2: create&push git tag to origin.',
                        action='store_true')
    args = parser.parse_args()
    custom_channel = 'ignore'
    build_type = 'Release'  # Default to release builds

    if args.debug:
        # For debug builds, use alpha flavor with debug build type
        flavors = ['alpha']
        targets = ['debug']
        build_type = 'Debug'
    elif args.alpha:d 'releases/versionName'
    - Pushes the git tag to origin for history
    - TODO (Not implemented yet): Uploads certain bits to releases.mediawiki.org: r, beta

To run
1) tell people on #wikimedia-mobile you're about to bump the version,
   so hold off on merging to main
2) git checkout main
3) git pull
4) git reset --hard
5) python scripts/make-release.py --prod (or --beta, --alpha, --debug)
Note: the apk file locations are printed to stdout
6) manual step: test the apk(s): adb install -r <apk file>
7) python scripts/make-release.py --prod --push
8) compile release note of prod using
    git log --pretty=format:"%h | %cr | %s" --abbrev-commit --no-merges `git tag -l r/*|tail -1`..
9) Upload prod apk to Play Store and to releases.mediawiki.org

For development/testing without signing setup, use: --debug
This builds a debug APK that can be installed for testing.

For Windows users experiencing file locking issues:
- Use --clear-cache to clear Gradle and lint caches before building
- Use --force-clean to forcefully clear the build directory  
- The script automatically retries on Windows file locking errors

Requires the environment variable ANDROID_HOME to run.
For production builds (--beta, --prod), also requires signing configuration in ~/.sign/signing.properties
Ensure you have a clean working directory before running as well.

See also https://www.mediawiki.org/wiki/Wikimedia_Apps/Team/Release_process
"""
import argparse
import glob
import os
import platform
import re
import shutil
import subprocess
import sys

PATH_PREFIX = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
# Use platform-specific gradlew command with proper path
if platform.system() == 'Windows':
    GRADLEW = os.path.join(PATH_PREFIX, 'gradlew.bat')
else:
    GRADLEW = os.path.join(PATH_PREFIX, 'gradlew')
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
    tag_name = get_git_tag_name(target, version_name)
    subprocess.run(['git', 'tag', '-a', tag_name, '-m', target], check=True)


def git_push_tag(target, version_name):
    """
    Pushes the git tag to origin
    """
    tag_name = get_git_tag_name(target, version_name)
    print('pushing tag ' + tag_name)
    subprocess.run(['git', 'push', 'origin', tag_name], check=True)


def make_release(flavors, custom_channel, build_type='Release'):
    return try_clean_build(GRADLEW, flavors, custom_channel, build_type)

def make_bundle(flavors, custom_channel, build_type='Release'):
    return try_clean_bundle(GRADLEW, flavors, custom_channel, build_type)


def get_output_apk_file_name(flavor, build_type='release'):
    build_type_lower = build_type.lower()
    return f'app/build/outputs/apk/{flavor}/{build_type_lower}/app-{flavor}-{build_type_lower}.apk'

def get_output_bundle_file_name(flavor, build_type='Release'):
    return f'app/build/outputs/bundle/{flavor}{build_type}/app-{flavor}-{build_type.lower()}.aab'


def verify_apk_exists(apk_path):
    """Verify that the APK file exists and is readable"""
    full_path = os.path.join(PATH_PREFIX, apk_path)
    if not os.path.exists(full_path):
        sys.exit(f"APK file not found: {full_path}")
    if not os.path.isfile(full_path):
        sys.exit(f"APK path is not a file: {full_path}")
    return full_path


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
    # Verify APK exists first
    full_apk_path = verify_apk_exists(apk_file)

    android_home = get_android_home()

    # Find the build-tools directory
    build_tools_path = os.path.join(android_home, 'build-tools')
    if not os.path.exists(build_tools_path):
        sys.exit(f"Build tools not found at {build_tools_path}")

    # Get the first available build tools version
    build_tools_versions = [d for d in os.listdir(build_tools_path) 
                          if os.path.isdir(os.path.join(build_tools_path, d))]
    if not build_tools_versions:
        sys.exit(f"No build tools versions found in {build_tools_path}")

    # Use the first available version
    build_tools_version = build_tools_versions[0]

    # Construct aapt path with platform-specific executable name
    aapt_name = 'aapt.exe' if platform.system() == 'Windows' else 'aapt'
    aapt = os.path.join(android_home, 'build-tools', build_tools_version, aapt_name)

    if not os.path.exists(aapt):
        sys.exit(f"aapt not found at {aapt}")

    try:
        process = subprocess.check_output([aapt, 'dump', 'badging', full_apk_path], stderr=subprocess.STDOUT)
        found = re.search(r'versionName=\'(\S+)\'', str(process))
        if found:
            apk_version_name = found.groups()[0]
            return apk_version_name
        else:
            sys.exit("Could not get version name from apk " + full_apk_path)
    except subprocess.CalledProcessError as e:
        print(f"Error running aapt on {full_apk_path}:")
        print(f"Command: {' '.join(e.cmd)}")
        print(f"Output: {e.output.decode() if e.output else 'None'}")
        sys.exit(f"aapt failed with exit code {e.returncode}")


def copy_apk(flavor, version_name, build_type='release'):
    folder_path = os.path.join(PATH_PREFIX, 'releases')
    os.makedirs(folder_path, exist_ok=True)
    build_suffix = '' if build_type.lower() == 'release' else f'-{build_type.lower()}'
    output_file = f'{folder_path}/wikipedia-{version_name}{build_suffix}.apk'
    source_apk = os.path.join(PATH_PREFIX, get_output_apk_file_name(flavor, build_type))
    shutil.copy2(source_apk, output_file)
    print(' apk: %s' % output_file)
    return output_file

def copy_bundle(flavor, version_name, build_type='Release'):
    folder_path = os.path.join(PATH_PREFIX, 'releases')
    os.makedirs(folder_path, exist_ok=True)
    output_file = f'{folder_path}/wikipedia-{version_name}.aab'
    source_bundle = os.path.join(PATH_PREFIX, get_output_bundle_file_name(flavor, build_type))
    shutil.copy2(source_bundle, output_file)
    print(' aab: %s' % output_file)


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


def check_signing_config(flavors):
    """Check if the required signing configuration exists for the given flavors"""
    # Flavors that require prod signing config
    prod_signing_flavors = ['beta', 'prod', 'custom']

    needs_prod_signing = any(flavor in prod_signing_flavors for flavor in flavors)

    if needs_prod_signing:
        # Check if production signing config exists
        prod_props_file = os.path.expanduser('~/.sign/signing.properties')
        if not os.path.exists(prod_props_file):
            print("ERROR: Production signing configuration required but not found.")
            print(f"Expected file: {prod_props_file}")
            print()
            print("To set up signing configuration:")
            print("1. Create the directory: mkdir -p ~/.sign")
            print("2. Copy the sample file:")
            print(f"   cp {os.path.join(PATH_PREFIX, 'app', 'signing.properties.sample')} {prod_props_file}")
            print("3. Edit the file with your actual keystore information")
            print()
            print("For development/testing, you can use debug builds which use the repo keystore.")
            sys.exit(1)
        else:
            print(f"Using production signing config: {prod_props_file}")
            # Validate the signing configuration
            if not validate_signing_config():
                sys.exit(1)
    else:
        # Check if repo keystore exists for debug signing
        repo_keystore = os.path.join(PATH_PREFIX, 'repo.keystore')
        if not os.path.exists(repo_keystore):
            print(f"WARNING: Repo keystore not found at {repo_keystore}")
            print("This may cause signing issues.")


def validate_signing_config():
    """Validate that the signing configuration is properly set up"""
    prod_props_file = os.path.expanduser('~/.sign/signing.properties')

    if not os.path.exists(prod_props_file):
        return False

    # Read and validate the properties file
    try:
        with open(prod_props_file, 'r') as f:
            content = f.read()

        # Check for common path issues
        if '/Users/' in content and platform.system() == 'Windows':
            print("WARNING: Found Unix-style path in signing.properties on Windows")
            print("Please update the keystore path to use Windows format:")
            print("Example: keystore=C:/path/to/your/keystore.jks")
            print()

        # Extract keystore path
        keystore_match = re.search(r'keystore\s*=\s*(.+)', content)
        if keystore_match:
            keystore_path = keystore_match.group(1).strip()

            # Convert relative paths to absolute if needed
            if not os.path.isabs(keystore_path):
                # Try relative to user home
                keystore_path = os.path.expanduser(keystore_path)

            # Check if keystore exists
            if not os.path.exists(keystore_path):
                print(f"ERROR: Keystore file not found: {keystore_path}")
                print("Please check the keystore path in your signing.properties file")
                return False
            else:
                print(f"✓ Keystore found: {keystore_path}")
                return True
        else:
            print("ERROR: No keystore path found in signing.properties")
            return False

    except Exception as e:
        print(f"ERROR: Failed to read signing.properties: {e}")
        return False


def try_clean_build(gradlew_cmd, flavors, custom_channel, build_type, max_retries=3):
    """Try to build with retries for Windows file locking issues"""
    # Ensure we're in the project root directory
    original_dir = os.getcwd()
    os.chdir(PATH_PREFIX)

    try:
        for attempt in range(max_retries):
            try:
                # Use relative path for gradlew when we're in the project root
                gradlew_exe = 'gradlew.bat' if platform.system() == 'Windows' else './gradlew'
                args = [gradlew_exe, '-q']

                if attempt > 0:
                    print(f"Retry attempt {attempt}/{max_retries - 1}")
                    # Skip clean on retries to avoid file locking issues
                    print("Skipping clean task due to previous file locking error...")
                else:
                    args.append('clean')

                args.append('-PcustomChannel=' + custom_channel)
                tasks = ['assemble{0}{1}'.format(flavor.title(), build_type) for flavor in flavors]
                args += tasks

                print(f"Running: {' '.join(args)}")
                result = subprocess.run(args, capture_output=True, text=True)

                if result.returncode == 0:
                    return result

                # Check for different types of file locking errors
                is_build_dir_lock = ('Unable to delete directory' in result.stderr or 
                                   'Failed to delete' in result.stderr or
                                   'cannot delete' in result.stderr.lower() or
                                   'access is denied' in result.stderr.lower())
                
                # More comprehensive lint cache lock detection
                is_lint_cache_lock = (
                    'lint-cache' in result.stderr or
                    'lint_models' in result.stderr or
                    'lint_partial_results' in result.stderr or
                    'incremental' in result.stderr or
                    ('cache' in result.stderr.lower() and ('delete' in result.stderr.lower() or 'process' in result.stderr.lower()))
                )
                
                if is_build_dir_lock or is_lint_cache_lock:
                    if attempt < max_retries - 1:
                        print("File locking error detected. This is common on Windows.")
                        
                        if is_lint_cache_lock:
                            print("Detected lint/cache file locking. Attempting comprehensive cleanup...")
                            kill_gradle_daemons()
                            clear_lint_cache()
                            # Also try to clear incremental build cache
                            try:
                                incremental_dir = os.path.join(PATH_PREFIX, 'app', 'build', 'intermediates', 'incremental')
                                if os.path.exists(incremental_dir):
                                    print("Clearing incremental build cache...")
                                    shutil.rmtree(incremental_dir, ignore_errors=True)
                            except:
                                pass
                        else:
                            print("General file lock detected, stopping Gradle daemons...")
                            kill_gradle_daemons()
                        
                        print("Waiting 8 seconds before retry (longer wait for cache issues)...")
                        import time
                        time.sleep(8)
                        continue

                # If it's not a file locking error, or final attempt, fail
                print("Build failed!")
                print("STDOUT:", result.stdout)
                print("STDERR:", result.stderr)
                sys.exit(f"Gradle build failed with exit code {result.returncode}")

            except Exception as e:
                if attempt < max_retries - 1:
                    print(f"Build attempt failed: {e}")
                    print("Retrying...")
                    continue
                else:
                    raise

        print("All retry attempts failed")
        sys.exit(1)
    finally:
        # Restore original directory
        os.chdir(original_dir)


def try_clean_bundle(gradlew_cmd, flavors, custom_channel, build_type, max_retries=3, skip_clean=True):
    """Try to build bundle with retries for Windows file locking issues"""
    # Ensure we're in the project root directory
    original_dir = os.getcwd()
    os.chdir(PATH_PREFIX)

    try:
        for attempt in range(max_retries):
            try:
                # Use relative path for gradlew when we're in the project root
                gradlew_exe = 'gradlew.bat' if platform.system() == 'Windows' else './gradlew'
                args = [gradlew_exe, '-q']

                if attempt > 0:
                    print(f"Bundle retry attempt {attempt}/{max_retries - 1}")
                    print("Skipping clean task due to previous file locking error...")
                    skip_clean = True

                # Skip clean by default for bundle builds to avoid file locking after APK build
                if not skip_clean:
                    args.append('clean')

                args.append('-PcustomChannel=' + custom_channel)
                tasks = ['bundle{0}{1}'.format(flavor.title(), build_type) for flavor in flavors]
                args += tasks

                print(f"Running: {' '.join(args)}")
                result = subprocess.run(args, capture_output=True, text=True)

                if result.returncode == 0:
                    return result

                # Check if it's a file locking error
                if 'Unable to delete directory' in result.stderr or 'Failed to delete' in result.stderr:
                    if attempt < max_retries - 1:
                        print("File locking error detected. This is common on Windows.")
                        print("Waiting 3 seconds before retry...")
                        import time
                        time.sleep(3)
                        skip_clean = True  # Skip clean on retry
                        continue

                # If it's not a file locking error, or final attempt, fail
                print("Bundle build failed!")
                print("STDOUT:", result.stdout)
                print("STDERR:", result.stderr)
                sys.exit(f"Gradle bundle build failed with exit code {result.returncode}")

            except Exception as e:
                if attempt < max_retries - 1:
                    print(f"Bundle build attempt failed: {e}")
                    print("Retrying...")
                    continue
                else:
                    raise

        print("All bundle retry attempts failed")
        sys.exit(1)
    finally:
        # Restore original directory
        os.chdir(original_dir)

def kill_gradle_daemons():
    """Kill any running Gradle daemons to release file locks"""
    try:
        print("Attempting to stop Gradle daemons...")
        # First try to stop daemons gracefully
        result = subprocess.run(['gradlew.bat', '--stop'], 
                              cwd=PATH_PREFIX, 
                              capture_output=True, 
                              text=True, 
                              timeout=30)
        if result.returncode == 0:
            print("✓ Gradle daemons stopped successfully")
        else:
            print("! Gradle daemon stop command completed with warnings")
    except subprocess.TimeoutExpired:
        print("! Gradle daemon stop timed out")
    except Exception as e:
        print(f"! Could not stop Gradle daemons: {e}")


def clear_lint_cache():
    """Clear Android Lint cache to resolve file locking issues"""
    try:
        # Multiple possible lint cache locations
        lint_cache_locations = [
            os.path.join(PATH_PREFIX, 'app', 'build', 'intermediates', 'lint-cache'),
            os.path.join(PATH_PREFIX, 'app', 'build', 'intermediates', 'lint_models'),
            os.path.join(PATH_PREFIX, 'app', 'build', 'intermediates', 'lint_partial_results'),
            os.path.join(PATH_PREFIX, 'app', 'build', 'intermediates', 'incremental'),
            os.path.join(PATH_PREFIX, 'app', 'build', 'tmp', 'lint')
        ]
        
        import time
        # Wait a moment for any processes to release files
        time.sleep(3)
        
        cleared_any = False
        for lint_dir in lint_cache_locations:
            if os.path.exists(lint_dir):
                print(f"Clearing lint cache: {lint_dir}")
                try:
                    # Force remove readonly files and handle Windows file locks
                    if platform.system() == 'Windows':
                        # Use takeown and icacls on Windows for stubborn files
                        subprocess.run(['takeown', '/f', lint_dir, '/r', '/d', 'y'], 
                                     capture_output=True, shell=True)
                        subprocess.run(['icacls', lint_dir, '/grant', 'administrators:F', '/t'], 
                                     capture_output=True, shell=True)
                    
                    shutil.rmtree(lint_dir, ignore_errors=True)
                    cleared_any = True
                except Exception as e:
                    print(f"! Warning: Could not fully clear {lint_dir}: {e}")
        
        if cleared_any:
            print("✓ Lint caches cleared")
        else:
            print("ℹ No lint cache found to clear")
            
    except Exception as e:
        print(f"! Could not clear lint cache: {e}")


def force_clear_build_directory():
    """Force clear the entire build directory on Windows"""
    try:
        build_dir = os.path.join(PATH_PREFIX, 'app', 'build')
        if os.path.exists(build_dir):
            print(f"Force clearing build directory: {build_dir}")
            import time
            time.sleep(2)
            
            if platform.system() == 'Windows':
                # Use Windows-specific commands to handle locked files
                subprocess.run(['takeown', '/f', build_dir, '/r', '/d', 'y'], 
                             capture_output=True, shell=True)
                subprocess.run(['icacls', build_dir, '/grant', 'administrators:F', '/t'], 
                             capture_output=True, shell=True)
            
            shutil.rmtree(build_dir, ignore_errors=True)
            print("✓ Build directory cleared")
        else:
            print("ℹ No build directory found to clear")
    except Exception as e:
        print(f"! Could not clear build directory: {e}")


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
    parser.add_argument('--debug',
                       help='Build debug APK (no signing required)',
                       action='store_true')
    parser.add_argument('--bundle',
                       help='Build a bundle (AAB) in addition to APK.',
                       action='store_true')
    parser.add_argument('--force-clean',
                       help='Force clean build directory before building (Windows file lock workaround)',
                       action='store_true')
    parser.add_argument('--clear-cache',
                       help='Clear Gradle and lint caches before building (helps with Windows file locks)',
                       action='store_true')
    group.add_argument('--channel',
                       help='Step 1: Custom versionName&channel. OEMs w/ Play')
    group.add_argument('--app',
                       help='Step 1: Custom versionName&channel. OEMs wout/ Play.')
    parser.add_argument('--push', help='Step 2: create&push git tag to origin.',
                        action='store_true')
    args = parser.parse_args()
    custom_channel = 'ignore'
    build_type = 'Release'  # Default to release builds

    if args.debug:
        # For debug builds, use alpha flavor with debug build type
        flavors = ['alpha']
        targets = ['debug']
        build_type = 'Debug'
    elif args.alpha:
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
        if custom_channel == 'ignore':
            label = targets[0]
        else:
            label = custom_channel
        apk_file = find_output_apk_for(label, get_version_code_from_build_file())
        version_name = get_version_name_from_apk(apk_file)
        for target in targets:
            git_tag(target, version_name)
            git_push_tag(target, version_name)
    else:
        # Check signing configuration before building (skip for debug builds)
        if build_type != 'Debug':
            check_signing_config(flavors)

        # Handle cache clearing and force clean options
        if args.clear_cache:
            print("Clearing caches before build...")
            kill_gradle_daemons()
            clear_lint_cache()
            
        if args.force_clean:
            print("Force clearing build directory...")
            kill_gradle_daemons()
            force_clear_build_directory()

        folder_path = 'releases'
        os.makedirs(folder_path, exist_ok=True)

        print('Building APK: ' + str(flavors) + f' ({build_type})')
        try_clean_build(GRADLEW, flavors, custom_channel, build_type)
        version_name = get_version_name_from_apk(get_output_apk_file_name(flavors[0], build_type))
        print('Copying APK...')
        output_file = copy_apk(flavors[0], version_name, build_type)

        if args.bundle and build_type == 'Release':  # Only build bundles for release builds
            print('Building bundle: ' + str(flavors))
            make_bundle(flavors, custom_channel, build_type)
            print('Copying bundle...')
            copy_bundle(flavors[0], version_name, build_type)

        """
        Remove the '.' to match the Samsung app store APK naming style.
        """
        if custom_channel == 'samsung':
            os.rename(output_file, output_file.replace('.apk', '').replace('.', '-') + '.apk')

        print('Please test the APK. After that, run w/ --push flag, and release the tested APK.')
        print('A useful command for collecting the release notes:')
        print('git log --pretty=format:"%h | %cr | %s" --abbrev-commit --no-merges ' +
              '`git tag --list ' + targets[0] + '/* | tail -1`..')


if __name__ == '__main__':
    main()
