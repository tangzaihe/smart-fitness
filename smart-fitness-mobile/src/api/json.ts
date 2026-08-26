/** Snowflake IDs lose precision if parsed as JS numbers. */
export function parseApiJson(text: string): unknown {
  const converted = text.replace(/([:\[,]\s*)(-?\d{16,})(?=\s*[,\}\]])/g, '$1"$2"');
  return JSON.parse(converted);
}

export function asId(value: unknown): string {
  if (value == null) {
    return '';
  }
  return String(value);
}

export function asNumber(value: unknown): number | null {
  if (value == null || value === '') {
    return null;
  }
  const n = typeof value === 'number' ? value : Number(value);
  return Number.isFinite(n) ? n : null;
}

export function recordOf(value: unknown): Record<string, unknown> {
  if (value && typeof value === 'object' && !Array.isArray(value)) {
    return value as Record<string, unknown>;
  }
  return {};
}

export const parseJson = parseApiJson;
export const asIdentifier = asId;
export const asNum = asNumber;
export const asRecord = recordOf;
