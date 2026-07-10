// SockJS expects Node's `global` in the browser. Must load before sockjs-client.
const g = globalThis as typeof globalThis & { global?: typeof globalThis };
if (typeof g.global === 'undefined') {
  g.global = g;
}
