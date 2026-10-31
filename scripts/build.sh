#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
./gradlew clean build
python3 scripts/package_ritual.py
python3 scripts/verify_baseline.py build/libs/harrower-neoforge-1.21.1-1.0.0.jar
