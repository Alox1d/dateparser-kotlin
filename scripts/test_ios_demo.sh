#!/bin/bash
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")/.."

# Use an explicit simulator when supplied; otherwise pick an available iPhone.
if [[ -z "${IOS_SIMULATOR_ID:-}" ]]; then
  IOS_SIMULATOR_ID="$(xcrun simctl list devices available -j | python3 -c '
import json, sys
devices = json.load(sys.stdin)["devices"]
phones = [d for runtime, group in devices.items() if ".iOS-" in runtime
          for d in group if d.get("isAvailable") and d["name"].startswith("iPhone")]
if not phones:
    sys.exit("No iPhone simulator is available. Install an iOS runtime in Xcode Settings > Components, then create a simulator.")
print(next((d for d in phones if d["state"] == "Booted"), phones[0])["udid"])
')"
fi

xcodebuild \
  -project integration/ios-app/DateParserDemo.xcodeproj \
  -scheme DateParserDemo -configuration Debug \
  -destination "platform=iOS Simulator,id=$IOS_SIMULATOR_ID" \
  -derivedDataPath integration/ios-app/build/DerivedData \
  -resultBundlePath "${IOS_TEST_RESULTS:-integration/ios-app/build/TestResults-$(date -u +%Y%m%dT%H%M%S).xcresult}" \
  CODE_SIGNING_ALLOWED=NO test
