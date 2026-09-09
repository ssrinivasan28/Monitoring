const TOKEN_KEY = 'sentinel_access_token';
const REFRESH_TOKEN_KEY = 'sentinel_refresh_token';
const ACTIVE_TENANT_KEY = 'sentinel_active_tenant_id';

export const getStoredToken = (): string | null => localStorage.getItem(TOKEN_KEY);
export const setStoredToken = (token: string): void => localStorage.setItem(TOKEN_KEY, token);

export const getStoredRefreshToken = (): string | null => localStorage.getItem(REFRESH_TOKEN_KEY);
export const setStoredRefreshToken = (token: string): void => localStorage.setItem(REFRESH_TOKEN_KEY, token);

export const getStoredTenantId = (): string | null => localStorage.getItem(ACTIVE_TENANT_KEY);
export const setStoredTenantId = (tenantId: string): void => localStorage.setItem(ACTIVE_TENANT_KEY, tenantId);

export const clearStoredAuth = (): void => {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(REFRESH_TOKEN_KEY);
  localStorage.removeItem(ACTIVE_TENANT_KEY);
};

export async function apiRequest<T = any>(
  endpoint: string,
  options: RequestInit = {}
): Promise<T> {
  const token = getStoredToken();
  const activeTenantId = getStoredTenantId();

  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options.headers as Record<string, string> || {}),
  };

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  if (activeTenantId) {
    headers['X-Tenant-Id'] = activeTenantId;
  }

  const response = await fetch(endpoint, {
    ...options,
    headers,
  });

  if (response.status === 401) {
    // Session expired or unauthenticated
    clearStoredAuth();
    if (!window.location.pathname.startsWith('/login')) {
      window.location.href = '/login?expired=true';
    }
    throw new Error('Unauthorized');
  }

  if (!response.ok) {
    let errorMessage = `HTTP ${response.status} ${response.statusText}`;
    try {
      const errorJson = await response.json();
      if (errorJson.message) {
        errorMessage = errorJson.message;
      } else if (errorJson.error) {
        errorMessage = errorJson.error;
      }
    } catch (_) {
      // Ignore JSON parse errors for non-json error responses
    }
    throw new Error(errorMessage);
  }

  // Handle empty responses like 204 No Content
  if (response.status === 204) {
    return {} as T;
  }

  return response.json();
}

export interface SseAssistantEvent {
  event: 'tool_call' | 'tool_result' | 'answer';
  data: any;
}

/**
 * Streams `POST /api/v1/assistant/chat/stream` (2.1) and invokes `onEvent` for each SSE frame as it
 * arrives. Can't use the browser's native `EventSource` here: it only issues GET requests and can't
 * carry the `Authorization`/`X-Tenant-Id` headers this endpoint requires, so the stream is parsed by
 * hand over `fetch()` + a `ReadableStream` reader instead.
 */
export async function streamAssistantChat(
  prompt: string,
  onEvent: (evt: SseAssistantEvent) => void
): Promise<void> {
  const token = getStoredToken();
  const activeTenantId = getStoredTenantId();

  const headers: Record<string, string> = { 'Content-Type': 'application/json' };
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }
  if (activeTenantId) {
    headers['X-Tenant-Id'] = activeTenantId;
  }

  const response = await fetch('/api/v1/assistant/chat/stream', {
    method: 'POST',
    headers,
    body: JSON.stringify({ prompt }),
  });

  if (response.status === 401) {
    clearStoredAuth();
    if (!window.location.pathname.startsWith('/login')) {
      window.location.href = '/login?expired=true';
    }
    throw new Error('Unauthorized');
  }

  // A role/entitlement failure (e.g. EntitlementAspect rejecting a Basic-tier caller) is returned as a
  // plain JSON body on the initial response, before any SSE frame is ever written - never feed that to
  // the frame parser below.
  const isEventStream = (response.headers.get('content-type') || '').includes('text/event-stream');
  if (!response.ok || !isEventStream || !response.body) {
    let message = `HTTP ${response.status} ${response.statusText}`;
    try {
      const errorJson = await response.json();
      message = errorJson.message || errorJson.error || message;
    } catch (_) {
      // Non-JSON error body; keep the generic message.
    }
    const error: any = new Error(message);
    error.status = response.status;
    throw error;
  }

  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let buffer = '';
  let sawAnswer = false;

  while (true) {
    const { done, value } = await reader.read();
    if (done) {
      break;
    }
    buffer += decoder.decode(value, { stream: true });

    let separatorIndex;
    while ((separatorIndex = buffer.indexOf('\n\n')) !== -1) {
      const frame = buffer.slice(0, separatorIndex);
      buffer = buffer.slice(separatorIndex + 2);
      const parsed = parseSseFrame(frame);
      if (parsed) {
        if (parsed.event === 'answer') {
          sawAnswer = true;
        }
        onEvent(parsed);
      }
    }
  }

  // The server-side SseEmitter has a 120s timeout (AssistantController.chatStream); if the connection
  // closes without a terminal `answer` frame (timeout or dropped connection), surface that as an error
  // rather than leaving the caller's UI stuck in a "streaming" state forever.
  if (!sawAnswer) {
    throw new Error('The Assistant stream ended before a final answer arrived. Please try again.');
  }
}

function parseSseFrame(frame: string): SseAssistantEvent | null {
  let eventName = '';
  const dataLines: string[] = [];
  for (const line of frame.split('\n')) {
    if (line.startsWith('event:')) {
      eventName = line.slice('event:'.length).trim();
    } else if (line.startsWith('data:')) {
      dataLines.push(line.slice('data:'.length).trim());
    }
  }
  if (!eventName || dataLines.length === 0) {
    return null;
  }
  try {
    return { event: eventName as SseAssistantEvent['event'], data: JSON.parse(dataLines.join('\n')) };
  } catch {
    return null;
  }
}
