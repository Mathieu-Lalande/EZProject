/**
 * Client HTTP minimal : cookie de session (same-origin) + en-tête CSRF lu depuis le cookie XSRF-TOKEN
 * (pattern cookie-to-header attendu par Spring Security).
 */
export class ApiError extends Error {
  readonly status: number;
  readonly fieldErrors: Record<string, string>;
  /** Code fonctionnel éventuel (ex. PASSWORD_CHANGE_REQUIRED, ACCOUNT_DISABLED, MFA_REQUIRED). */
  readonly code: string | null;

  constructor(status: number, message: string, fieldErrors: Record<string, string> = {}, code: string | null = null) {
    super(message);
    this.status = status;
    this.fieldErrors = fieldErrors;
    this.code = code;
  }
}

function readCookie(name: string): string | null {
  const match = document.cookie.split('; ').find((c) => c.startsWith(`${name}=`));
  return match ? decodeURIComponent(match.slice(name.length + 1)) : null;
}

const SAFE_METHODS = new Set(['GET', 'HEAD', 'OPTIONS']);

export async function request<T = void>(method: string, url: string, body?: unknown, form?: URLSearchParams | FormData): Promise<T> {
  const headers: Record<string, string> = {};
  if (!SAFE_METHODS.has(method)) {
    const token = readCookie('XSRF-TOKEN');
    if (token) headers['X-XSRF-TOKEN'] = token;
  }
  let payload: BodyInit | undefined;
  if (form) {
    payload = form;
  } else if (body !== undefined) {
    headers['Content-Type'] = 'application/json';
    payload = JSON.stringify(body);
  }

  const res = await fetch(url, { method, headers, body: payload, credentials: 'same-origin' });

  if (!res.ok) {
    let message = res.status === 401 ? 'Session expirée' : `Erreur ${res.status}`;
    let fieldErrors: Record<string, string> = {};
    let code: string | null = null;
    try {
      const problem = (await res.json()) as { detail?: string; errors?: Record<string, string>; code?: string };
      if (problem.detail) message = problem.detail;
      if (problem.errors) fieldErrors = problem.errors;
      if (problem.code) code = problem.code;
    } catch {
      // Réponse sans corps JSON
    }
    throw new ApiError(res.status, message, fieldErrors, code);
  }
  if (res.status === 204 || res.headers.get('content-length') === '0') {
    return undefined as T;
  }
  const text = await res.text();
  return (text ? JSON.parse(text) : undefined) as T;
}

export const api = {
  get: <T>(url: string) => request<T>('GET', url),
  post: <T>(url: string, body?: unknown) => request<T>('POST', url, body),
  put: <T>(url: string, body?: unknown) => request<T>('PUT', url, body),
  patch: <T>(url: string, body?: unknown) => request<T>('PATCH', url, body),
  delete: <T = void>(url: string) => request<T>('DELETE', url),
  postForm: <T>(url: string, form: URLSearchParams) => request<T>('POST', url, undefined, form),
  /** Envoi multipart : le navigateur fixe lui-même le Content-Type et la boundary. */
  upload: <T>(url: string, data: FormData) => request<T>('POST', url, undefined, data),
};
