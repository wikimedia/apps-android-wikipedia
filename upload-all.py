#!/usr/bin/env python
"""
Cross-platform script to upload Wikipedia Android APKs to releases server.

This script uploads beta and stable release APKs to the Wikimedia releases server
using SCP. It's a Python equivalent of the upload-all.sh bash script.

Usage:
    python upload-all.py [options]

Options:
    --dry-run    Show what would be uploaded without actually uploading
    --verbose    Show detailed output during upload
    --help       Show this help message

Requirements:
    - SCP must be available in PATH (or specify path with --scp-path)
    - SSH key authentication must be set up for releases.discovery.wmnet
    - APK files must exist in the releases/ directory

The script uploads:
    - Beta APKs (wikipedia-*-beta*.apk) to betas/ directory
    - Stable APKs (wikipedia-*-r*.apk) to stable/ directory
"""

import argparse
import glob
import os
import platform
import subprocess
import sys

# Base configuration
RELEASES_DIR = 'releases'
REMOTE_HOST = 'releases.discovery.wmnet'
REMOTE_BASE_PATH = '/srv/org/wikimedia/releases/mobile/android/wikipedia'
BETA_REMOTE_PATH = f'{REMOTE_BASE_PATH}/betas/'
STABLE_REMOTE_PATH = f'{REMOTE_BASE_PATH}/stable/'


def find_scp_command():
    """Find the SCP command on different platforms"""
    if platform.system() == 'Windows':
        # On Windows, try common locations for SCP
        possible_paths = [
            'scp',  # If it's in PATH
            'C:/Windows/System32/OpenSSH/scp.exe',
            'C:/Program Files/Git/usr/bin/scp.exe',
            'C:/Program Files (x86)/Git/usr/bin/scp.exe'
        ]
        
        for scp_path in possible_paths:
            try:
                result = subprocess.run([scp_path, '-V'], 
                                      capture_output=True, 
                                      text=True, 
                                      timeout=5)
                if result.returncode == 0 or 'OpenSSH' in result.stderr:
                    return scp_path
            except (subprocess.TimeoutExpired, FileNotFoundError):
                continue
        
        print("ERROR: SCP not found. Please install OpenSSH or Git for Windows.")
        print("Possible solutions:")
        print("1. Install Git for Windows (includes SCP)")
        print("2. Install Windows OpenSSH feature")
        print("3. Use WSL (Windows Subsystem for Linux)")
        sys.exit(1)
    else:
        # On Unix-like systems, SCP should be in PATH
        try:
            result = subprocess.run(['which', 'scp'], 
                                  capture_output=True, 
                                  text=True)
            if result.returncode == 0:
                return 'scp'
        except FileNotFoundError:
            pass
        
        print("ERROR: SCP not found. Please install OpenSSH client.")
        sys.exit(1)


def find_apk_files(pattern):
    """Find APK files matching the given pattern"""
    search_pattern = os.path.join(RELEASES_DIR, pattern)
    files = glob.glob(search_pattern)
    return [f for f in files if f.endswith('.apk')]


def upload_files(files, remote_path, scp_cmd, dry_run=False, verbose=False):
    """Upload files to remote server using SCP"""
    if not files:
        print(f"No files found to upload to {remote_path}")
        return True
    
    print(f"\nUploading {len(files)} file(s) to {remote_path}:")
    for file in files:
        print(f"  - {file}")
    
    if dry_run:
        print("  [DRY RUN] Would execute:")
        for file in files:
            print(f"    {scp_cmd} {file} {REMOTE_HOST}:{remote_path}")
        return True
    
    # Upload files
    success = True
    for file in files:
        cmd = [scp_cmd, file, f'{REMOTE_HOST}:{remote_path}']
        
        if verbose:
            print(f"Executing: {' '.join(cmd)}")
        
        try:
            result = subprocess.run(cmd, check=True, capture_output=not verbose)
            if verbose:
                print(f"✓ Successfully uploaded {file}")
        except subprocess.CalledProcessError as e:
            print(f"✗ Failed to upload {file}: {e}")
            success = False
        except KeyboardInterrupt:
            print(f"\n✗ Upload interrupted by user")
            success = False
            break
    
    return success


def validate_environment():
    """Validate that the environment is set up correctly"""
    # Check if releases directory exists
    if not os.path.exists(RELEASES_DIR):
        print(f"ERROR: Releases directory '{RELEASES_DIR}' not found.")
        print("Please run this script from the Wikipedia Android project root directory.")
        return False
    
    # Check if there are any APK files at all
    all_apks = find_apk_files('*.apk')
    if not all_apks:
        print(f"WARNING: No APK files found in '{RELEASES_DIR}' directory.")
        print("Please build some APKs first using make-release.py")
    
    return True


def main():
    parser = argparse.ArgumentParser(
        description='Upload Wikipedia Android APKs to releases server',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
    python upload-all.py                    # Upload all APKs
    python upload-all.py --dry-run          # Show what would be uploaded
    python upload-all.py --verbose          # Show detailed output
    python upload-all.py --scp-path /usr/bin/scp  # Use specific SCP path

Note: You must have SSH key authentication set up for releases.discovery.wmnet
        """
    )
    
    parser.add_argument('--dry-run', 
                       action='store_true',
                       help='Show what would be uploaded without actually uploading')
    
    parser.add_argument('--verbose', '-v',
                       action='store_true', 
                       help='Show detailed output during upload')
    
    parser.add_argument('--scp-path',
                       help='Path to SCP executable (auto-detected if not specified)')
    
    args = parser.parse_args()
    
    # Validate environment
    if not validate_environment():
        sys.exit(1)
    
    # Find SCP command
    if args.scp_path:
        scp_cmd = args.scp_path
        # Verify the specified SCP path works
        try:
            subprocess.run([scp_cmd, '-V'], capture_output=True, timeout=5)
        except (subprocess.TimeoutExpired, FileNotFoundError):
            print(f"ERROR: Specified SCP path '{scp_cmd}' is not valid.")
            sys.exit(1)
    else:
        scp_cmd = find_scp_command()
    
    print(f"Using SCP: {scp_cmd}")
    
    # Find beta and stable APK files
    beta_files = find_apk_files('wikipedia-*-beta*.apk')
    stable_files = find_apk_files('wikipedia-*-r*.apk')
    
    # Show summary
    print(f"\nFound {len(beta_files)} beta APK(s) and {len(stable_files)} stable APK(s)")
    
    if not beta_files and not stable_files:
        print("No APK files found to upload.")
        sys.exit(0)
    
    if args.dry_run:
        print("\n=== DRY RUN MODE ===")
    
    # Upload files
    all_success = True
    
    if beta_files:
        success = upload_files(beta_files, BETA_REMOTE_PATH, scp_cmd, args.dry_run, args.verbose)
        all_success = all_success and success
    
    if stable_files:
        success = upload_files(stable_files, STABLE_REMOTE_PATH, scp_cmd, args.dry_run, args.verbose)
        all_success = all_success and success
    
    # Final status
    if args.dry_run:
        print("\n=== DRY RUN COMPLETE ===")
        print("Use without --dry-run to actually upload the files.")
    elif all_success:
        print("\n✓ All uploads completed successfully!")
    else:
        print("\n✗ Some uploads failed. Please check the output above.")
        sys.exit(1)


if __name__ == '__main__':
    main()
