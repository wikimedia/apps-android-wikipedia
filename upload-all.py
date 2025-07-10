#!/usr/bin/env python
"""
Uploads all built APKs to the Wikimedia releases server.

Requirements:
    - SCP must be available in PATH
    - SSH key authentication must be set up for releases.discovery.wmnet
    - APK files must exist in the releases/ directory
"""

import glob
import subprocess
import sys

# Upload configuration
UPLOAD_CONFIGS = [
    {
        'name': 'beta',
        'pattern': './releases/wikipedia-*-beta*.apk',
        'path': 'releases.discovery.wmnet:/srv/org/wikimedia/releases/mobile/android/wikipedia/betas/'
    },
    {
        'name': 'stable',
        'pattern': './releases/wikipedia-*-r*.apk',
        'path': 'releases.discovery.wmnet:/srv/org/wikimedia/releases/mobile/android/wikipedia/stable/'
    }
]


def upload_apks(name, pattern, upload_path):
    """Upload APK files matching the pattern to the specified path"""
    files = glob.glob(pattern)
    if files:
        try:
            subprocess.run(['scp'] + files + [upload_path], check=True)
            return True
        except subprocess.CalledProcessError as e:
            print(f"✗ Failed to upload {name} APKs: {e}")
            sys.exit(1)
    else:
        print(f"No {name} APKs found to upload")
        return False


def main():
    upload_count = 0

    for config in UPLOAD_CONFIGS:
        if upload_apks(config['name'], config['pattern'], config['path']):
            upload_count += 1

    if upload_count == 0:
        print("No APK files found to upload.")
        sys.exit(1)

    print(f"\n✓ {upload_count} uploads completed successfully!")


if __name__ == '__main__':
    main()
