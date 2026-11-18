/** Reject dangerous schemes (`javascript:`, `data:`, etc.) in user-configured URLs */
const SAFE_URL_PROTOCOLS = new Set(['http:', 'https:'])

/**
 * Validate and normalize a URL: allows only http(s) and same-origin relative paths,
 * returning an empty string for unsafe or malformed URLs.
 */
export function safeUrl(url: string | null | undefined): string {
  if (!url?.trim())
    return ''
  try {
    const parsed = new URL(url.trim(), window.location.origin)
    return SAFE_URL_PROTOCOLS.has(parsed.protocol) ? parsed.href : ''
  }
  catch {
    return ''
  }
}
