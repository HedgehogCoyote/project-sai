const BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080').replace(/\/$/, '')

export class ApiError extends Error {
  constructor(message: string, public readonly status: number, public readonly code?: string) {
    super(message)
    this.name = 'ApiError'
  }
}

export async function apiRequest<T>(path: string, options: RequestInit = {}): Promise<T> {
  const headers = new Headers(options.headers)
  if (options.body) headers.set('Content-Type', 'application/json')

  let response: Response
  try {
    response = await fetch(`${BASE_URL}${path}`, { ...options, headers, credentials: 'include' })
  } catch {
    throw new ApiError('서버에 연결할 수 없습니다. 잠시 후 다시 시도해 주세요.', 0)
  }

  if (!response.ok) {
    let body: { message?: string; code?: string } = {}
    try { body = (await response.json()) as typeof body } catch { /* empty response */ }
    throw new ApiError(body.message ?? '요청을 처리하지 못했습니다.', response.status, body.code)
  }
  if (response.status === 204) return undefined as T
  return (await response.json()) as T
}
