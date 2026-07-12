/** Map API/HTTP failures to short user-facing copy (avoid raw "Forbidden"). */
export function friendlyHttpError(err: unknown, fallback: string): string {
  const e = err as {
    status?: number;
    error?: string | { error?: string; message?: string };
    message?: string;
  } | null;

  const status = e?.status;
  const body = e?.error;
  const raw = (
    typeof body === 'string'
      ? body
      : body?.error || body?.message || e?.message || ''
  )
    .toString()
    .trim();
  const lower = raw.toLowerCase();

  if (status === 403 || lower === 'forbidden' || lower.includes('access denied') || lower.includes('not allowed')) {
    return 'This section is restricted for your role';
  }
  if (status === 401 || lower === 'unauthorized') {
    return 'Sign in again to continue';
  }
  if (status === 404) {
    return 'Nothing found here yet';
  }
  if (status === 0 || lower.includes('unknown error')) {
    return 'Network error — check connection and retry';
  }
  if (raw && lower !== 'forbidden' && lower !== 'unauthorized') {
    return raw;
  }
  return fallback;
}
