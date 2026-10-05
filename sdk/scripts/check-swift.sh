#!/usr/bin/env bash
set -euo pipefail

sdk_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
framework="${sdk_root}/trivia-sdk-core/build/XCFrameworks/release/TriviaCore.xcframework"
source="${sdk_root}/samples/swift-consumer/Smoke.swift"

if [[ ! -d "$framework" ]]; then
    echo "Missing ${framework}; run :trivia-sdk-core:assembleTriviaCoreReleaseXCFramework first." >&2
    exit 1
fi

for slice in ios-arm64 ios-arm64-simulator; do
    binary="${framework}/${slice}/TriviaCore.framework/TriviaCore"
    if [[ ! -f "$binary" ]]; then
        echo "Missing XCFramework slice: ${binary}" >&2
        exit 1
    fi
    architectures="$(xcrun lipo -archs "$binary")"
    if [[ "$architectures" != "arm64" ]]; then
        echo "Unexpected architectures in ${slice}: ${architectures}" >&2
        exit 1
    fi
    if [[ "$slice" == "ios-arm64-simulator" ]]; then
        sdk=iphonesimulator
        target=arm64-apple-ios15.0-simulator
    else
        sdk=iphoneos
        target=arm64-apple-ios15.0
    fi
    xcrun --sdk "$sdk" swiftc -typecheck \
        -sdk "$(xcrun --sdk "$sdk" --show-sdk-path)" \
        -target "$target" -F "${framework}/${slice}" "$source"
done

echo "TriviaCore Swift API smoke check passed for device and Apple Silicon simulator."
