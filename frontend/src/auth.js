import { reactive } from 'vue'
import Keycloak from 'keycloak-js'

const keycloak = new Keycloak({
    url: import.meta.env.VITE_KEYCLOAK_URL || 'http://localhost:8080',
    realm: 'logistics',
    clientId: 'logistics-web',
})
const redirectUri = `${window.location.origin}${window.location.pathname}`

// Токены хранит адаптер в памяти; в localStorage их не записываем.
export const auth = reactive({ ready: false, authenticated: false, username: '', admin: false, error: '' })

function syncUser() {
    auth.authenticated = Boolean(keycloak.authenticated)
    auth.username = keycloak.tokenParsed?.preferred_username || ''
    auth.admin = auth.authenticated && keycloak.hasRealmRole('ADMIN')
}

keycloak.onAuthSuccess = syncUser
keycloak.onAuthRefreshSuccess = syncUser
keycloak.onAuthLogout = syncUser
keycloak.onTokenExpired = () => { getAccessToken().catch(() => {}) }

export async function initializeAuth() {
    try {
        await keycloak.init({ onLoad: 'check-sso', pkceMethod: 'S256', checkLoginIframe: false })
        syncUser()
    } catch {
        auth.error = 'Не удалось подключиться к Keycloak. Проверьте запуск backend и обновите страницу.'
    } finally {
        auth.ready = true
    }
}

export async function login() {
    try { await keycloak.login({ redirectUri }) }
    catch { auth.error = 'Не удалось открыть страницу входа.' }
}

export async function logout() {
    try { await keycloak.logout({ redirectUri }) }
    catch { auth.error = 'Не удалось выйти. Попробуйте ещё раз.' }
}

export function clearSession() {
    keycloak.clearToken()
    syncUser()
    auth.error = 'Сессия завершилась. Войдите снова.'
}

export async function getAccessToken() {
    if (!keycloak.authenticated) throw new Error('Войдите в систему.')
    try { await keycloak.updateToken(30) }
    catch { clearSession(); throw new Error(auth.error) }
    return keycloak.token
}
