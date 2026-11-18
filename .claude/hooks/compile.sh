#!/bin/bash
root="$(git rev-parse --show-toplevel)" || exit 2
exec node "$root/scripts/agent/compile.mjs"
