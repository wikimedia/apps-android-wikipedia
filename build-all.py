#!/usr/bin/env python
"""
Cross-platform build script for all release variants.
Compatible with Windows, macOS, and Linux.
"""

import os
import sys
import subprocess
import platform

def run_make_release(*args):
    """Run make-release.py with the given arguments"""
    cmd = [sys.executable, 'scripts/make-release.py'] + list(args)
    
    print(f"Building with arguments: {' '.join(args)}")
    
    try:
        result = subprocess.run(cmd, check=True)
        print(f"Build completed successfully: {' '.join(args)}")
        print()
        return True
    except subprocess.CalledProcessError as e:
        print(f"Failed to build with arguments: {' '.join(args)}")
        print(f"Exit code: {e.returncode}")
        return False
    except Exception as e:
        print(f"Unexpected error: {e}")
        return False

def main():
    """Main build process"""
    print("Starting build-all process...")
    print("This will build multiple release variants sequentially.")
    print(f"Platform: {platform.system()} {platform.release()}")
    print()
    
    # Check if we're in the right directory
    if not os.path.exists('scripts/make-release.py'):
        print("Error: scripts/make-release.py not found!")
        print("   Please run this script from the project root directory.")
        sys.exit(1)
    
    builds = [
        # (description, args)
        ("Beta bundle", ["--beta", "--bundle"]),
        ("Production bundle", ["--prod", "--bundle"]),
        ("Amazon APK", ["--channel", "amazon"]),
        ("Samsung APK", ["--channel", "samsung"]),
        ("Huawei APK", ["--channel", "huawei"]),
    ]
    
    success_count = 0
    total_builds = len(builds)
    
    for description, args in builds:
        print(f"Building {description}...")
        if run_make_release(*args):
            success_count += 1
        else:
            print(f"Build failed for {description}")
            print("   Stopping build process.")
            break
    
    print("=" * 50)
    if success_count == total_builds:
        print(f"All {total_builds} builds completed successfully!")
        print("Check the releases/ directory for output files.")
    else:
        print(f" {success_count}/{total_builds} builds completed successfully.")
        sys.exit(1)

if __name__ == "__main__":
    main()
