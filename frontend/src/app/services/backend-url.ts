/** Backend host derived from the page URL so LAN/public IP access works. */
export function backendOrigin(): string {
  const { protocol, hostname } = window.location;
  const httpProtocol = protocol === 'https:' ? 'https:' : 'http:';
  return `${httpProtocol}//${hostname}:8080`;
}

export function apiBaseUrl(): string {
  return `${backendOrigin()}/api`;
}

export function wsUrl(): string {
  return `${backendOrigin()}/ws`;
}
