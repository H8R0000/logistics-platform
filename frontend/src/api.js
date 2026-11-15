import { clearSession, getAccessToken } from './auth.js'

export async function api(service, path, { method = 'GET', body } = {}) {
    const token = await getAccessToken()
    let response
    try {
        response = await fetch(`/${service}/api/v1${path}`, {
            method,
            headers: {
                Authorization: `Bearer ${token}`,
                ...(body === undefined ? {} : { 'Content-Type': 'application/json' }),
            },
            body: body === undefined ? undefined : JSON.stringify(body),
            signal: AbortSignal.timeout(10000),
            cache: 'no-store',
        })
    } catch {
        throw new Error(method === 'POST'
            ? 'Нет ответа сервера. Обновите список перед повтором операции: она могла выполниться.'
            : 'Нет связи с сервером. Проверьте, что backend запущен.')
    }
    const result = await response.json().catch(() => null)
    if (!response.ok) {
        if (response.status === 401) { clearSession(); throw new Error('Сессия завершилась. Войдите снова.') }
        if (response.status === 403) throw new Error('Недостаточно прав для этой операции.')
        const fields = Object.entries(result?.errors || {}).map(([field, message]) => `${field}: ${message}`)
        throw new Error(fields.join('; ') || result?.detail || `Ошибка сервера (${response.status}).`)
    }
    return result
}
