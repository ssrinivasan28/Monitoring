#!/usr/bin/env bash
# show-prompt.sh — assemble the shared preamble + a build chunk and copy to clipboard.
# Usage:  ./show-prompt.sh 0.9        (copies preamble + chunk 0.9 to the clipboard)
#         ./show-prompt.sh 1.2 --print (also prints to stdout)
set -euo pipefail

id="${1:?usage: show-prompt.sh <chunk-id, e.g. 0.9> [--print]}"
dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
preamble="$dir/00-shared-preamble.md"
[ -f "$preamble" ] || { echo "Preamble not found: $preamble" >&2; exit 1; }

chunk="$(find "$dir" -name "${id}-*.md" | head -1)"
[ -n "$chunk" ] || { echo "No chunk file matching '${id}-*.md' under $dir" >&2; exit 1; }

out="$(cat "$preamble"; printf '\n\n---\n\n'; cat "$chunk")"

if   command -v clip.exe >/dev/null 2>&1; then printf '%s' "$out" | clip.exe
elif command -v pbcopy   >/dev/null 2>&1; then printf '%s' "$out" | pbcopy
elif command -v xclip    >/dev/null 2>&1; then printf '%s' "$out" | xclip -selection clipboard
else echo "(no clipboard tool found; printing)"; printf '%s\n' "$out"; fi

echo "Copied preamble + $(basename "$chunk") to clipboard. Paste into a fresh Claude Code session on IPSentinel."
[ "${2:-}" = "--print" ] && printf '%s\n' "$out" || true
