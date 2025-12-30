#!/bin/bash

# Script to pull Allure results from Android device and generate report

set -e

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ALLURE_RESULTS_DIR="$PROJECT_DIR/allure-results"
ALLURE_REPORT_DIR="$PROJECT_DIR/allure-report"

echo "🔍 Searching for Allure results on device..."

# Common locations where Allure Android stores results
# With TestStorage (orchestrator): /sdcard/googletest/test_outputfiles/allure-results
# Without TestStorage: /data/data/org.wikipedia.test/files/allure-results
POSSIBLE_LOCATIONS=(
    "/sdcard/googletest/test_outputfiles/allure-results"
    "/storage/emulated/0/googletest/test_outputfiles/allure-results"
    "/data/data/org.wikipedia.test/files/allure-results"
    "/data/local/tmp/allure-results"
    "/sdcard/allure-results"
    "/storage/emulated/0/allure-results"
    "/data/data/org.wikipedia.test/cache/allure-results"
)

FOUND_LOCATION=""

# Check each location
for location in "${POSSIBLE_LOCATIONS[@]}"; do
    echo "Checking: $location"
    if adb shell "test -d $location && ls $location" 2>/dev/null | grep -q .; then
        FOUND_LOCATION="$location"
        echo "✅ Found results at: $location"
        break
    fi
done

if [ -z "$FOUND_LOCATION" ]; then
    echo "⚠️  Allure results not found in common locations."
    echo "Checking if results are in build directory..."
    
    # Check if results are already in build directory
    if [ -d "$PROJECT_DIR/app/build/outputs/allure-results" ]; then
        echo "✅ Found results in build directory"
        FOUND_LOCATION="local:$PROJECT_DIR/app/build/outputs/allure-results"
    elif [ -d "$PROJECT_DIR/allure-results" ]; then
        echo "✅ Found results in project root"
        FOUND_LOCATION="local:$PROJECT_DIR/allure-results"
    else
        echo "❌ Could not find Allure results."
        echo ""
        echo "Please check:"
        echo "1. Did the tests complete successfully?"
        echo "2. Check device logs: adb logcat | grep -i allure"
        echo "3. Manually check device: adb shell 'find /data -name *allure* -type d'"
        exit 1
    fi
fi

# Pull results from device if found on device
if [[ "$FOUND_LOCATION" != local:* ]]; then
    echo ""
    echo "📥 Pulling results from device..."
    mkdir -p "$ALLURE_RESULTS_DIR"
    
    # Check if it's TestStorage location (orchestrator)
    if [[ "$FOUND_LOCATION" == *"googletest"* ]]; then
        echo "   Using TestStorage location (orchestrator)..."
        # Use the official method from Allure docs
        # Extract directly to results dir, not into a subdirectory
        adb exec-out sh -c "cd $(dirname $FOUND_LOCATION) && tar cf - $(basename $FOUND_LOCATION)" | tar xvf - -C "$ALLURE_RESULTS_DIR" --strip-components=0 2>/dev/null || {
            echo "⚠️  Trying alternative method..."
            # Pull to temp dir first, then move contents
            TEMP_DIR=$(mktemp -d)
            adb pull "$FOUND_LOCATION" "$TEMP_DIR/" 2>/dev/null
            # Move files from nested structure if needed
            if [ -d "$TEMP_DIR/allure-results" ]; then
                mv "$TEMP_DIR/allure-results"/* "$ALLURE_RESULTS_DIR/" 2>/dev/null
                rmdir "$TEMP_DIR/allure-results" 2>/dev/null
            else
                mv "$TEMP_DIR"/* "$ALLURE_RESULTS_DIR/" 2>/dev/null
            fi
            rmdir "$TEMP_DIR" 2>/dev/null
        }
    elif [[ "$FOUND_LOCATION" == *"/data/data/"* ]]; then
        echo "   Using app files directory..."
        # For app files directory, use run-as method
        PACKAGE_NAME=$(echo "$FOUND_LOCATION" | sed -n 's|.*/data/data/\([^/]*\).*|\1|p')
        if [ -n "$PACKAGE_NAME" ]; then
            adb exec-out run-as "$PACKAGE_NAME" sh -c "cd /data/data/$PACKAGE_NAME/files && tar cf - allure-results" | tar xvf - -C "$ALLURE_RESULTS_DIR" 2>/dev/null || {
                echo "⚠️  Trying direct pull..."
                adb pull "$FOUND_LOCATION" "$ALLURE_RESULTS_DIR"
            }
        else
            adb pull "$FOUND_LOCATION" "$ALLURE_RESULTS_DIR"
        fi
    else
        # Standard pull for other locations
        # Pull to temp dir first to avoid nested structure
        TEMP_DIR=$(mktemp -d)
        adb pull "$FOUND_LOCATION" "$TEMP_DIR/" || {
            echo "⚠️  Failed to pull from device, trying alternative method..."
            adb shell "tar -czf /sdcard/allure-results.tar.gz -C $(dirname $FOUND_LOCATION) $(basename $FOUND_LOCATION)" 2>/dev/null
            adb pull /sdcard/allure-results.tar.gz "$PROJECT_DIR/" 2>/dev/null
            if [ -f "$PROJECT_DIR/allure-results.tar.gz" ]; then
                tar -xzf "$PROJECT_DIR/allure-results.tar.gz" -C "$TEMP_DIR"
                rm "$PROJECT_DIR/allure-results.tar.gz"
            fi
        }
        # Move files from nested structure if needed, avoiding duplicate nesting
        if [ -d "$TEMP_DIR/allure-results" ]; then
            mv "$TEMP_DIR/allure-results"/* "$ALLURE_RESULTS_DIR/" 2>/dev/null
            rmdir "$TEMP_DIR/allure-results" 2>/dev/null
        else
            mv "$TEMP_DIR"/* "$ALLURE_RESULTS_DIR/" 2>/dev/null
        fi
        rmdir "$TEMP_DIR" 2>/dev/null
    fi
else
    # Use local path
    local_path="${FOUND_LOCATION#local:}"
    if [ "$local_path" != "$ALLURE_RESULTS_DIR" ]; then
        echo "📋 Copying results to $ALLURE_RESULTS_DIR..."
        mkdir -p "$ALLURE_RESULTS_DIR"
        cp -r "$local_path"/* "$ALLURE_RESULTS_DIR/" 2>/dev/null || true
    fi
fi

# Fix nested directory structure if results ended up in a subdirectory
if [ -d "$ALLURE_RESULTS_DIR/allure-results" ] && [ "$(ls -A $ALLURE_RESULTS_DIR/allure-results 2>/dev/null)" ]; then
    echo "📋 Fixing nested directory structure..."
    # Move files from nested directory to parent
    mv "$ALLURE_RESULTS_DIR/allure-results"/* "$ALLURE_RESULTS_DIR/" 2>/dev/null
    rmdir "$ALLURE_RESULTS_DIR/allure-results" 2>/dev/null
    echo "✅ Fixed nested structure"
fi

# Check if we have results
if [ ! -d "$ALLURE_RESULTS_DIR" ] || [ -z "$(ls -A $ALLURE_RESULTS_DIR 2>/dev/null)" ]; then
    echo "❌ No Allure results found after pulling."
    echo "   Checked directory: $ALLURE_RESULTS_DIR"
    if [ -d "$ALLURE_RESULTS_DIR/allure-results" ]; then
        echo "   Found nested directory but it appears empty"
    fi
    exit 1
fi

echo ""
echo "✅ Allure results found: $ALLURE_RESULTS_DIR"
echo "   Files: $(ls -1 $ALLURE_RESULTS_DIR | wc -l | tr -d ' ')"

# Check if Allure CLI is installed
if ! command -v allure &> /dev/null; then
    echo ""
    echo "⚠️  Allure CLI is not installed."
    echo "Install it with:"
    echo "  macOS:   brew install allure"
    echo "  Linux:   sudo apt-add-repository ppa:qameta/allure && sudo apt-get update && sudo apt-get install allure"
    echo "  Windows: scoop install allure"
    echo ""
    echo "Or use Docker:"
    echo "  docker run --rm -v \"$PROJECT_DIR:/app\" -w /app qameta/allure allure serve allure-results"
    exit 1
fi

echo ""
echo "📊 Generating Allure report..."
allure generate "$ALLURE_RESULTS_DIR" -o "$ALLURE_REPORT_DIR" --clean

# Skip opening browser in CI environments
if [ -z "$CI" ] && [ -z "$GITHUB_ACTIONS" ]; then
    echo ""
    echo "🌐 Opening Allure report..."
    allure open "$ALLURE_REPORT_DIR"
else
    echo ""
    echo "ℹ️  Running in CI environment, skipping browser open"
    # Create tar.gz for CI
    cd "$PROJECT_DIR"
    tar -czf allure-report.tar.gz allure-report 2>/dev/null || true
fi

echo ""
echo "✅ Done! Report generated successfully."
echo "   Report location: $ALLURE_REPORT_DIR"
echo "   Results location: $ALLURE_RESULTS_DIR"

