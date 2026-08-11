#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
exec bb test/paid_outreach/contracts_test.clj
