/**
 * Findings are stored as seconds and displayed as `0:42` / `4:15`, the format the mockup uses.
 * Both clients render and parse them, so the pair lives here.
 */

/** 42 -> "0:42", 255 -> "4:15", 3675 -> "1:01:15". */
export function formatTimestamp(totalSeconds: number): string {
  const safe = Math.max(0, Math.floor(totalSeconds));
  const hours = Math.floor(safe / 3600);
  const minutes = Math.floor((safe % 3600) / 60);
  const seconds = safe % 60;

  const paddedSeconds = String(seconds).padStart(2, '0');
  if (hours > 0) {
    return `${hours}:${String(minutes).padStart(2, '0')}:${paddedSeconds}`;
  }
  return `${minutes}:${paddedSeconds}`;
}

/** "4:15" -> 255. Returns null when the input is not a timestamp. */
export function parseTimestamp(value: string): number | null {
  const parts = value.trim().split(':');
  if (parts.length < 2 || parts.length > 3) {
    return null;
  }

  const numbers = parts.map((part) => Number(part));
  if (numbers.some((n) => !Number.isInteger(n) || n < 0)) {
    return null;
  }

  return numbers.reduce((total, part) => total * 60 + part, 0);
}
