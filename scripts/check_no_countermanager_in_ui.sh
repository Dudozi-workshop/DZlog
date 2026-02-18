#!/usr/bin/env bash
set -euo pipefail

# SSOT guard: ui layer should not directly call CounterManager.
# Allowed path is coordinator/policy layer.
if rg -n "CounterManager\.getNextCounter\(" app/src/main/java/com/example/dzlog/ui; then
  echo "[FAIL] CounterManager direct usage found in ui layer. Use TableCounterPolicyCoordinator instead." >&2
  exit 1
fi

echo "[PASS] No CounterManager direct usage in ui layer."
