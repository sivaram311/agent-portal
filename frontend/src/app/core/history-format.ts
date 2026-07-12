/**
 * Provider-agnostic History event formatting (Cursor ACP + Antigravity print/ACP).
 * Prefer human labels over raw JSON; keep unknown shapes readable without dumping blobs.
 */

export function formatHistoryEvent(
  type: string,
  payload: Record<string, unknown> | null | undefined
): { title: string; detail: string } {
  const p = payload ?? {};
  switch (type) {
    case 'tool_call':
    case 'subagent_started':
    case 'subagent_progress':
    case 'subagent_finished': {
      const name = str(p['toolName'] ?? p['name'] ?? p['title']) || 'tool';
      const status = str(p['status']) || '—';
      const kind = str(p['kind']);
      const title =
        type === 'tool_call' ? name : `${shortType(type)} · ${name}`;
      const parts = [status];
      if (kind) {
        parts.push(kind);
      }
      const args = summarizeArgs(p['args'] ?? p['argsJson']);
      if (args) {
        parts.push(args);
      }
      return { title, detail: parts.join(' · ') };
    }
    case 'terminal_chunk': {
      const stream = str(p['stream']) || 'out';
      const text = str(p['text']).replace(/\s+/g, ' ').trim();
      return {
        title: `terminal · ${stream}`,
        detail: clip(text || '(empty)', 200),
      };
    }
    case 'status':
      return { title: 'status', detail: str(p['status']) || '—' };
    case 'session_title':
      return { title: 'session title', detail: str(p['title']) || '—' };
    case 'session_update': {
      const update = str(p['sessionUpdate']);
      const titleFromPayload = str(p['title']);
      if (titleFromPayload) {
        return { title: 'session update', detail: `title → ${titleFromPayload}` };
      }
      const fromRaw = parseNestedRaw(p['raw']);
      const nestedTitle = fromRaw ? str(fromRaw['title']) : '';
      if (nestedTitle) {
        return { title: 'session update', detail: `title → ${nestedTitle}` };
      }
      if (fromRaw && str(fromRaw['sessionUpdate']) === 'available_commands_update') {
        const cmds = fromRaw['availableCommands'];
        const n = Array.isArray(cmds) ? cmds.length : 0;
        return { title: 'commands update', detail: `${n} commands available` };
      }
      return {
        title: update ? `session · ${update}` : 'session update',
        detail: clip(summarizeObject(fromRaw ?? p), 180),
      };
    }
    case 'assistant_message':
      return {
        title: 'assistant',
        detail: clip(str(p['content'] ?? p['text']), 240),
      };
    case 'assistant_delta':
    case 'thinking_delta':
      return {
        title: type,
        detail: clip(str(p['text']), 160),
      };
    case 'permission_required':
    case 'permission_auto_approved':
    case 'permission_resolved':
    case 'plan_required': {
      const decision = str(p['decision'] ?? p['outcome'] ?? p['status']);
      const tool = str(p['toolName'] ?? p['toolCallId']);
      return {
        title: shortType(type),
        detail: [decision, tool].filter(Boolean).join(' · ') || summarizeObject(p),
      };
    }
    case 'run_completed':
    case 'run_cancelled':
    case 'run_failed':
      return {
        title: shortType(type),
        detail:
          str(p['stopReason'] ?? p['error'] ?? p['message']) ||
          summarizeObject(p) ||
          '—',
      };
    case 'input_required':
      return {
        title: 'input required',
        detail: clip(str(p['prompt'] ?? p['message']), 200),
      };
    case 'bridge_ready':
      return {
        title: 'bridge ready',
        detail: str(p['workspacePath'] ?? p['provider']) || 'connected',
      };
    case 'conversation_bound':
      return {
        title: 'conversation',
        detail: str(p['conversationId']) || 'bound',
      };
    case 'cursor_task':
    case 'cursor_update_todos':
      return {
        title: shortType(type),
        detail: clip(str(p['description'] ?? p['raw'] ?? summarizeObject(p)), 180),
      };
    default:
      return {
        title: type || 'event',
        detail: clip(summarizeObject(p), 200),
      };
  }
}

function shortType(type: string): string {
  return type.replace(/_/g, ' ');
}

function str(v: unknown): string {
  if (v == null) {
    return '';
  }
  if (typeof v === 'string') {
    return v;
  }
  if (typeof v === 'number' || typeof v === 'boolean') {
    return String(v);
  }
  return '';
}

function clip(text: string, max: number): string {
  const t = (text || '').trim();
  if (t.length <= max) {
    return t;
  }
  return t.slice(0, max - 1) + '…';
}

function summarizeArgs(args: unknown): string {
  if (args == null) {
    return '';
  }
  let raw = typeof args === 'string' ? args.trim() : JSON.stringify(args);
  if (!raw || raw === '{}' || raw === 'null' || raw === '[]') {
    return '';
  }
  try {
    const obj = typeof args === 'string' ? JSON.parse(args) : args;
    if (obj && typeof obj === 'object' && !Array.isArray(obj)) {
      const rec = obj as Record<string, unknown>;
      const hint =
        str(rec['_toolName'] ?? rec['toolName'] ?? rec['command'] ?? rec['description'] ?? rec['prompt']);
      if (hint) {
        return clip(hint, 80);
      }
      const keys = Object.keys(rec);
      if (keys.length) {
        return clip(keys.slice(0, 4).join(', '), 80);
      }
    }
  } catch {
    /* keep raw */
  }
  return clip(raw, 80);
}

function parseNestedRaw(raw: unknown): Record<string, unknown> | null {
  if (raw == null) {
    return null;
  }
  if (typeof raw === 'object' && !Array.isArray(raw)) {
    return raw as Record<string, unknown>;
  }
  if (typeof raw === 'string') {
    try {
      const parsed = JSON.parse(raw);
      if (parsed && typeof parsed === 'object' && !Array.isArray(parsed)) {
        return parsed as Record<string, unknown>;
      }
    } catch {
      return null;
    }
  }
  return null;
}

function summarizeObject(obj: Record<string, unknown>): string {
  const skip = new Set(['raw', 'args', 'content']);
  const bits: string[] = [];
  for (const [k, v] of Object.entries(obj)) {
    if (skip.has(k) || v == null) {
      continue;
    }
    if (typeof v === 'string' || typeof v === 'number' || typeof v === 'boolean') {
      bits.push(`${k}=${v}`);
    } else if (Array.isArray(v)) {
      bits.push(`${k}[${v.length}]`);
    }
    if (bits.length >= 4) {
      break;
    }
  }
  if (bits.length) {
    return bits.join(' · ');
  }
  try {
    return JSON.stringify(obj);
  } catch {
    return '';
  }
}
