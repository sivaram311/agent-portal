import { formatHistoryEvent } from './history-format';

describe('formatHistoryEvent', () => {
  it('formats Cursor-style tool_call without dumping JSON', () => {
    const r = formatHistoryEvent('tool_call', {
      toolName: 'Shell',
      status: 'completed',
      kind: 'execute',
      args: { command: 'uname -a', _toolName: 'Shell' },
    });
    expect(r.title).toBe('Shell');
    expect(r.detail).toContain('completed');
    expect(r.detail).not.toContain('{');
  });

  it('formats Antigravity terminal_chunk', () => {
    const r = formatHistoryEvent('terminal_chunk', {
      stream: 'stdout',
      text: 'hello from agy\n',
    });
    expect(r.title).toBe('terminal · stdout');
    expect(r.detail).toContain('hello from agy');
  });

  it('summarizes session_update available_commands', () => {
    const r = formatHistoryEvent('session_update', {
      raw: JSON.stringify({
        sessionUpdate: 'available_commands_update',
        availableCommands: [{}, {}, {}],
      }),
    });
    expect(r.title).toBe('commands update');
    expect(r.detail).toContain('3 commands');
  });

  it('falls back for unknown types without raw stringify of huge blobs', () => {
    const r = formatHistoryEvent('custom_thing', { a: 1, b: 'x', raw: 'nope' });
    expect(r.title).toBe('custom_thing');
    expect(r.detail).toContain('a=1');
  });
});
