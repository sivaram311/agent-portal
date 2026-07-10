/** Backend host derived from the page URL so LAN/public IP and reverse-proxy access work. */
export function backendOrigin(): string {
  const { protocol, hostname, port } = window.location;
  const httpProtocol = protocol === 'https:' ? 'https:' : 'http:';
  // Standard ports (nginx reverse proxy) — API/WS are same-origin (/api, /ws).
  if (!port || port === '80' || port === '443') {
    return `${httpProtocol}//${hostname}`;
  }
  // Direct ng serve / IP:4200 — API on sibling :8080
  return `${httpProtocol}//${hostname}:8080`;
}

export function apiBaseUrl(): string {
  return `${backendOrigin()}/api`;
}

export function wsUrl(): string {
  return `${backendOrigin()}/ws`;
}
