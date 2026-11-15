<script setup>
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { auth, initializeAuth, login, logout } from './auth.js'
import { api } from './api.js'
import { deliveryTypes, eventTitles, shipmentStatuses, stages, taskStatuses } from './labels.js'

const shipments = ref([])
const selectedId = ref(null)
const search = ref('')
const page = ref(0)
const totalElements = ref(0)
const totalPages = ref(0)
const statistics = ref(null)
const notice = ref('')
const error = ref('')
const detailError = ref('')
const taskError = ref('')
const loading = ref(false)
const refreshing = ref(false)
const detailLoading = ref(false)
const detailsLoaded = ref(false)
const submitting = ref(false)
const retrying = ref(false)
const shipmentEvents = ref([])
const selectedTask = ref(null)
const taskEvents = ref([])
const createDialog = ref(null)
const formError = ref('')
const defaults = () => ({ status: '', deliveryType: '', createdFrom: '', createdTo: '', sort: 'createdAt,desc' })
const filters = reactive(defaults())
const appliedFilters = ref(defaults())
let listRequest = 0
let detailRequest = 0
let timer

function emptyForm() {
    return {
        origin: { city: '', country: '', address: '' },
        destination: { city: '', country: '', address: '' },
        weight: 1, dimensions: { length: 10, width: 10, height: 10 }, deliveryType: 'EXPRESS',
    }
}
const form = reactive(emptyForm())
const filteredShipments = computed(() => {
    const query = search.value.trim().toLocaleLowerCase('ru')
    return shipments.value.filter((s) => `${s.id} ${s.origin.city} ${s.destination.city}`.toLocaleLowerCase('ru').includes(query))
})
const selectedShipment = computed(() => shipments.value.find((s) => s.id === selectedId.value) || null)
const activeCount = computed(() => ['QUEUED', 'PROCESSING', 'WAITING_FOR_COURIER', 'IN_TRANSIT']
    .reduce((sum, status) => sum + (statistics.value?.byStatus[status] || 0), 0))
const deliveredCount = computed(() => statistics.value?.byStatus.COMPLETED || 0)
const failedCount = computed(() => (statistics.value?.byStatus.FAILED || 0) + (statistics.value?.byStatus.COURIER_NOT_FOUND || 0))
const extraFilterCount = computed(() => [appliedFilters.value.createdFrom, appliedFilters.value.createdTo,
    appliedFilters.value.sort !== 'createdAt,desc', search.value.trim()].filter(Boolean).length)
const canRetry = computed(() => auth.admin && ['FAILED', 'COURIER_NOT_FOUND'].includes(selectedTask.value?.status))
const taskIsActive = computed(() => selectedTask.value && !['FAILED', 'COURIER_NOT_FOUND', 'COMPLETED'].includes(selectedTask.value.status))

function statusMeta(status, task = false) {
    return (task ? taskStatuses : shipmentStatuses)[status] || { label: status, tone: 'neutral' }
}
function shortId(id) { return id.slice(0, 8) }
function formatDate(value) {
    if (!value) return '—'
    return new Intl.DateTimeFormat('ru-RU', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit', second: '2-digit' }).format(new Date(value))
}
function formatNumber(value) { return new Intl.NumberFormat('ru-RU', { maximumFractionDigits: 3 }).format(value) }

async function loadDetails() {
    const id = selectedId.value
    const request = ++detailRequest
    if (!id || !auth.authenticated) return
    // При фоновом обновлении сохраняем содержимое и не показываем загрузку заново.
    detailLoading.value = !detailsLoaded.value
    const current = () => request === detailRequest && id === selectedId.value && auth.authenticated
    try {
        const [shipment, history, tasks] = await Promise.allSettled([
            api('management', `/shipments/${id}`), api('management', `/shipments/${id}/events`),
            api('processing', `/tasks?shipmentId=${id}&size=1`),
        ])
        if (!current()) return
        let nextDetailError = ''
        let nextTaskError = ''
        if (shipment.status === 'fulfilled') {
            const index = shipments.value.findIndex((s) => s.id === id)
            if (index >= 0) shipments.value[index] = shipment.value
        } else { nextDetailError = shipment.reason.message }
        if (history.status === 'fulfilled') shipmentEvents.value = history.value
        else nextDetailError = history.reason.message
        if (tasks.status === 'fulfilled') {
            selectedTask.value = tasks.value.content[0] || null
            if (selectedTask.value) {
                try {
                    const events = await api('processing', `/tasks/${selectedTask.value.id}/events`)
                    if (current()) taskEvents.value = events
                } catch (e) { nextTaskError = e.message }
            } else taskEvents.value = []
        } else { nextTaskError = tasks.reason.message }
        if (current()) {
            detailError.value = nextDetailError
            taskError.value = nextTaskError
            detailsLoaded.value = true
        }
    } finally { if (current()) detailLoading.value = false }
}

async function refresh({ preferredId = null, force = false, background = false } = {}) {
    if (!auth.authenticated || (refreshing.value && !force)) return
    const request = ++listRequest
    refreshing.value = true
    loading.value = !background
    try {
        const params = new URLSearchParams({ page: page.value, size: 10, sort: appliedFilters.value.sort })
        for (const field of ['status', 'deliveryType', 'createdFrom', 'createdTo']) {
            const value = appliedFilters.value[field]
            if (value) params.set(field, field.startsWith('created') ? new Date(value).toISOString() : value)
        }
        const result = await api('management', `/shipments?${params}`)
        if (request !== listRequest || !auth.authenticated) return
        // Последняя страница могла исчезнуть из-за изменения статуса заявки.
        if (page.value > 0 && page.value >= Math.max(1, result.totalPages)) {
            page.value = Math.max(0, result.totalPages - 1)
            return await refresh({ preferredId, force: true, background })
        }
        const previousId = selectedId.value
        shipments.value = result.content
        totalElements.value = result.totalElements
        totalPages.value = result.totalPages
        if (preferredId && result.content.some((s) => s.id === preferredId)) selectedId.value = preferredId
        else if (!result.content.some((s) => s.id === selectedId.value)) selectedId.value = result.content[0]?.id || null
        if (selectedId.value && selectedId.value === previousId) await loadDetails()
        if (request !== listRequest || !auth.authenticated) return
        let refreshError = ''
        if (auth.admin) {
            try {
                const result = await api('processing', '/tasks/statistics')
                if (request === listRequest && auth.authenticated) statistics.value = result
            } catch (e) { refreshError = e.message }
        }
        if (request === listRequest && auth.authenticated) error.value = refreshError
    } catch (e) { if (request === listRequest) error.value = e.message }
    finally { if (request === listRequest) { loading.value = false; refreshing.value = false } }
}

function applyFilters() { appliedFilters.value = { ...filters }; page.value = 0; refresh({ force: true }) }
function clearFilters() { Object.assign(filters, defaults()); search.value = ''; applyFilters() }
function changePage(offset) { page.value += offset; refresh({ force: true }) }

watch(filteredShipments, (rows) => {
    if (!rows.some((s) => s.id === selectedId.value)) selectedId.value = rows[0]?.id || null
})
watch(selectedId, () => {
    selectedTask.value = null; shipmentEvents.value = []; taskEvents.value = []
    detailsLoaded.value = false
    detailError.value = ''; taskError.value = ''
    if (selectedId.value) loadDetails()
    else { detailRequest++; detailLoading.value = false }
})
watch(() => auth.authenticated, (authenticated) => {
    if (authenticated) { page.value = 0; refresh() }
    else {
        listRequest++; detailRequest++; loading.value = false; refreshing.value = false; detailLoading.value = false
        shipments.value = []; selectedId.value = null; statistics.value = null
        createDialog.value?.close()
    }
}, { immediate: true })
onMounted(async () => {
    if (!auth.ready) await initializeAuth()
    timer = window.setInterval(() => {
        if (!document.hidden && !submitting.value && !retrying.value && !detailLoading.value && !createDialog.value?.open) refresh({ background: true })
    }, 5000)
})
onUnmounted(() => { clearInterval(timer); listRequest++; detailRequest++ })

function openCreateForm() { Object.assign(form, emptyForm()); formError.value = ''; createDialog.value.showModal() }
function fillExample() {
    Object.assign(form, emptyForm(), {
        origin: { city: 'Vilnius', country: 'Lithuania', address: 'Gedimino pr. 1' },
        destination: { city: 'Berlin', country: 'Germany', address: 'Unter den Linden 1' },
    })
}
function cancelCreate(event) { if (submitting.value) event.preventDefault() }
async function createShipment() {
    if (submitting.value) return
    const address = (value) => Object.fromEntries(Object.entries(value).map(([k, v]) => [k, v.trim()]))
    const origin = address(form.origin), destination = address(form.destination)
    if ([...Object.values(origin), ...Object.values(destination)].some((v) => !v)) {
        formError.value = 'Заполните город, страну и адрес для обеих точек.'; return
    }
    submitting.value = true; formError.value = ''
    try {
        const result = await api('management', '/shipments', { method: 'POST', body: {
            origin, destination, weight: form.weight, dimensions: { ...form.dimensions }, deliveryType: form.deliveryType,
        } })
        createDialog.value?.close()
        notice.value = `Заявка ${shortId(result.id)} создана. Обработка начнётся автоматически.`
        Object.assign(filters, defaults()); appliedFilters.value = defaults(); page.value = 0; search.value = ''
        await refresh({ preferredId: result.id, force: true })
    } catch (e) { formError.value = e.message }
    finally { submitting.value = false }
}
async function retryTask() {
    if (!canRetry.value || retrying.value) return
    const id = selectedTask.value.id
    retrying.value = true; taskError.value = ''
    try {
        selectedTask.value = await api('processing', `/tasks/${id}/retry`, { method: 'POST' })
        notice.value = 'Повтор обработки принят.'
        await loadDetails()
    } catch (e) { taskError.value = e.message }
    finally { retrying.value = false }
}
</script>

<template>
    <header class="topbar">
        <div class="topbar-inner">
            <a class="brand" href="#main" aria-label="Logistics — к списку заявок">
                <img src="/favicon.svg" alt="" width="40" height="40" />
                <span>Logistics<small>Платформа доставки</small></span>
            </a>
            <div v-if="auth.authenticated" class="account-controls">
                <span class="account-label">{{ auth.username }} · {{ auth.admin ? 'ADMIN' : 'USER' }}</span>
                <button class="button button-secondary" @click="logout">Выйти</button>
            </div>
        </div>
    </header>

    <main v-if="!auth.authenticated" class="page">
        <section class="panel login-panel">
            <h1>Logistics Platform</h1>
            <p class="subtitle">Войдите, чтобы создавать заявки и следить за доставками.</p>
            <p v-if="auth.error" class="form-error" role="alert">{{ auth.error }}</p>
            <p v-if="!auth.ready" class="subtitle" role="status">Подключение…</p>
            <button v-else class="button button-primary" @click="login">Войти</button>
        </section>
    </main>
    <main v-else id="main" class="page">
        <section class="page-heading" aria-labelledby="page-title">
            <div>
                <h1 id="page-title">{{ auth.admin ? 'Все заявки' : 'Мои заявки' }}</h1>
            </div>
            <button class="button button-primary" :disabled="submitting" @click="openCreateForm">
                <span class="plus" aria-hidden="true">+</span> Новая заявка
            </button>
        </section>

        <div v-if="notice" class="notice" role="status">
            <span><span aria-hidden="true">✓</span> {{ notice }}</span>
            <button class="icon-button" aria-label="Закрыть уведомление" @click="notice = ''">×</button>
        </div>

        <div v-if="error" class="notice error-notice" role="alert">{{ error }}</div>
        <section v-if="auth.admin" class="statistics" aria-label="Статистика обработки">
            <span>Задачи обработки</span>
            <span>Всего <strong>{{ statistics?.total ?? '—' }}</strong></span>
            <span>Активные <strong>{{ statistics ? activeCount : '—' }}</strong></span>
            <span>Завершены <strong>{{ statistics ? deliveredCount : '—' }}</strong></span>
            <span>Неудачные <strong>{{ statistics ? failedCount : '—' }}</strong></span>
        </section>

        <div class="workspace">
            <section class="panel shipments-panel" aria-labelledby="shipments-title">
                <div class="panel-heading">
                    <div class="heading-with-count">
                        <h2 id="shipments-title">Заявки</h2><span class="count-badge">{{ totalElements }}</span>
                    </div>
                    <button class="button button-secondary refresh-button" :disabled="loading || submitting" @click="refresh({ force: true })">{{ loading ? 'Обновление…' : 'Обновить' }}</button>
                </div>

                <form class="filters" @submit.prevent="applyFilters">
                    <div class="filter-row">
                        <select v-model="filters.status" aria-label="Фильтр по статусу">
                            <option value="">Все статусы</option>
                            <option v-for="(status, value) in shipmentStatuses" :key="value" :value="value">{{ status.label }}</option>
                        </select>
                        <select v-model="filters.deliveryType" aria-label="Тип доставки"><option value="">Любая доставка</option><option v-for="(label, value) in deliveryTypes" :key="value" :value="value">{{ label }}</option></select>
                        <button class="button button-secondary" type="submit" :disabled="loading || submitting">Применить</button>
                        <button class="text-button" type="button" :disabled="loading || submitting" @click="clearFilters">Сбросить</button>
                    </div>
                    <details class="extra-filters">
                        <summary>Ещё фильтры<span v-if="extraFilterCount"> · {{ extraFilterCount }}</span></summary>
                        <div class="extra-filter-fields">
                            <label class="field-wide">Поиск на текущей странице<input v-model="search" type="search" placeholder="ID или город" aria-label="Поиск на текущей странице" /></label>
                            <label>Создана с<input v-model="filters.createdFrom" type="datetime-local" /></label>
                            <label>Создана по<input v-model="filters.createdTo" type="datetime-local" /></label>
                            <label class="field-wide">Сортировка<select v-model="filters.sort" aria-label="Сортировка"><option value="createdAt,desc">Сначала новые</option><option value="createdAt,asc">Сначала старые</option><option value="weight,asc">По весу ↑</option><option value="weight,desc">По весу ↓</option></select></label>
                        </div>
                    </details>
                </form>

                <div v-if="filteredShipments.length" class="table-scroll">
                    <table>
                        <thead><tr><th>Заявка</th><th>Маршрут</th><th>Статус</th><th class="weight-column">Вес</th></tr></thead>
                        <tbody>
                            <tr v-for="shipment in filteredShipments" :key="shipment.id"
                                :class="{ 'is-selected': selectedShipment?.id === shipment.id }"
                                @click="selectedId = shipment.id">
                                <td>
                                    <button class="shipment-link" :aria-pressed="selectedShipment?.id === shipment.id"
                                        @click.stop="selectedId = shipment.id">{{ shortId(shipment.id) }}</button>
                                    <span class="cell-caption">{{ formatDate(shipment.createdAt) }}</span>
                                </td>
                                <td>
                                    <span class="route-city">{{ shipment.origin.city }} <span class="route-arrow" aria-hidden="true">→</span></span>
                                    <span class="destination-city">{{ shipment.destination.city }}</span>
                                    <span class="cell-caption">{{ deliveryTypes[shipment.deliveryType] }}</span>
                                </td>
                                <td><span class="status-badge" :class="statusMeta(shipment.status).tone"><span class="status-dot"></span>{{ statusMeta(shipment.status).label }}</span></td>
                                <td class="weight-column weight-value">{{ formatNumber(shipment.weight) }} <span>кг</span></td>
                            </tr>
                        </tbody>
                    </table>
                </div>
                <div v-else class="empty-state">
                    <div class="empty-symbol" aria-hidden="true">⌕</div>
                    <h3>{{ loading ? 'Загрузка заявок…' : 'Заявки не найдены' }}</h3>
                    <p>Создайте заявку или измените фильтры.</p>
                    <button class="button button-secondary" @click="clearFilters">Сбросить фильтры</button>
                </div>

                <div class="list-footer">
                    <span>На странице {{ filteredShipments.length }} · всего {{ totalElements }}</span>
                    <div class="pagination"><button class="button button-secondary" :disabled="page === 0 || loading || submitting" @click="changePage(-1)">←</button><span>{{ page + 1 }} / {{ Math.max(1, totalPages) }}</span><button class="button button-secondary" :disabled="page + 1 >= totalPages || loading || submitting" @click="changePage(1)">→</button></div>
                </div>
            </section>

            <aside class="panel details-panel" aria-labelledby="details-title">
                <template v-if="selectedShipment">
                    <div class="details-heading">
                        <h2 id="details-title">Заявка {{ shortId(selectedShipment.id) }}</h2>
                        <span class="status-badge" :class="statusMeta(selectedShipment.status).tone"><span class="status-dot"></span>{{ statusMeta(selectedShipment.status).label }}</span>
                    </div>

                    <div class="route-detail">
                        <div class="route-stop">
                            <span class="stop-marker origin-marker" aria-hidden="true"></span>
                            <div><p class="field-caption">Откуда</p><h3>{{ selectedShipment.origin.city }}, {{ selectedShipment.origin.country }}</h3><p>{{ selectedShipment.origin.address }}</p></div>
                        </div>
                        <div class="route-stop">
                            <span class="stop-marker destination-marker" aria-hidden="true"></span>
                            <div><p class="field-caption">Куда</p><h3>{{ selectedShipment.destination.city }}, {{ selectedShipment.destination.country }}</h3><p>{{ selectedShipment.destination.address }}</p></div>
                        </div>
                    </div>

                    <dl class="shipment-facts">
                        <div><dt>Тип доставки</dt><dd>{{ deliveryTypes[selectedShipment.deliveryType] }}</dd></div>
                        <div><dt>Вес посылки</dt><dd>{{ formatNumber(selectedShipment.weight) }} кг</dd></div>
                        <div class="fact-wide"><dt>Габариты, Д × Ш × В</dt><dd>{{ formatNumber(selectedShipment.dimensions.length) }} × {{ formatNumber(selectedShipment.dimensions.width) }} × {{ formatNumber(selectedShipment.dimensions.height) }} см</dd></div>
                        <div v-if="selectedShipment.deliveredAt" class="fact-wide"><dt>Доставлена</dt><dd>{{ formatDate(selectedShipment.deliveredAt) }}</dd></div>
                    </dl>

                    <details class="technical-details">
                        <summary>Данные заявки</summary>
                        <dl class="task-facts">
                            <div><dt>ID заявки</dt><dd class="id-text">{{ selectedShipment.id }}</dd></div>
                            <div v-if="auth.admin"><dt>Владелец</dt><dd class="id-text">{{ selectedShipment.ownerId }}</dd></div>
                        </dl>
                    </details>

                    <section class="task-section" aria-label="Обработка заявки">
                        <p v-if="taskError" class="form-error" role="alert">{{ taskError }}</p>
                        <details class="task-details">
                            <summary>Обработка заявки<span v-if="selectedTask" class="task-status">{{ statusMeta(selectedTask.status, true).label }}</span></summary>
                            <template v-if="selectedTask">
                                <dl class="task-facts">
                                    <div><dt>Этап</dt><dd>{{ stages[selectedTask.currentStage] }}</dd></div>
                                    <div><dt>Неудачных попыток подряд</dt><dd>{{ selectedTask.attempts }}</dd></div>
                                    <div v-if="taskIsActive"><dt>Следующее выполнение</dt><dd>{{ formatDate(selectedTask.nextExecutionAt) }}</dd></div>
                                </dl>
                                <details class="task-history"><summary>История обработки ({{ taskEvents.length }})</summary><ol class="task-event-list"><li v-for="event in taskEvents" :key="event.id"><strong>{{ eventTitles[event.eventType] || event.eventType }}</strong><time>{{ formatDate(event.createdAt) }}</time><p>{{ event.details }}</p></li></ol></details>
                            </template>
                            <p v-else-if="!taskError" class="subtitle">{{ detailLoading ? 'Загрузка задачи…' : 'Задача ещё не зарегистрирована.' }}</p>
                        </details>
                        <p v-if="selectedTask?.errorMessage" class="form-error">{{ selectedTask.errorMessage }}</p>
                        <button v-if="canRetry" class="button button-primary retry-button" :disabled="retrying || detailLoading" @click="retryTask">{{ retrying ? 'Отправка…' : 'Повторить обработку' }}</button>
                    </section>
                    <section class="history-section" aria-labelledby="history-title">
                        <h3 id="history-title">История доставки <span>{{ shipmentEvents.length }}</span></h3>
                        <p v-if="detailError" class="form-error" role="alert">{{ detailError }}</p>
                        <p v-if="!shipmentEvents.length && !detailError" class="subtitle">{{ detailLoading ? 'Загрузка истории…' : 'История пока пуста.' }}</p>
                        <ol class="timeline">
                            <li v-for="(event, index) in shipmentEvents" :key="event.id"
                                :class="{ latest: index === shipmentEvents.length - 1, completed: selectedShipment.status === 'DELIVERED' }">
                                <span class="timeline-marker" aria-hidden="true"></span>
                                <p>{{ eventTitles[event.eventType] || event.eventType }}</p><time :datetime="event.createdAt">{{ formatDate(event.createdAt) }}</time>
                                <details v-if="event.details" class="event-description"><summary>Подробности</summary><p class="event-details">{{ event.details }}</p></details>
                            </li>
                        </ol>
                    </section>
                </template>
                <div v-else class="empty-state details-empty"><h2 id="details-title">Здесь будут подробности</h2><p>Выберите заявку из списка.</p></div>
            </aside>
        </div>

        <footer class="page-footer">
            <p>Статусы обновляются автоматически каждые 5 секунд.</p>
        </footer>
    </main>

    <dialog v-if="auth.authenticated" ref="createDialog" class="create-dialog" aria-labelledby="create-title" @cancel="cancelCreate" @close="formError = ''">
        <form @submit.prevent="createShipment">
            <div class="dialog-heading">
                <h2 id="create-title">Создать заявку</h2>
                <button type="button" class="icon-button" aria-label="Закрыть форму" :disabled="submitting" @click="createDialog.close()">×</button>
            </div>
            <div class="dialog-body"><button type="button" class="text-button example-button" :disabled="submitting" @click="fillExample">Заполнить пример</button>
                <div class="address-grid">
                    <fieldset>
                        <legend><span class="section-number">1</span> Откуда забрать</legend>
                        <div class="address-fields">
                            <label>Город<input v-model="form.origin.city" name="origin-city" placeholder="Вильнюс" maxlength="100" required /></label>
                            <label>Страна<input v-model="form.origin.country" name="origin-country" placeholder="Литва" maxlength="100" required /></label>
                            <label class="field-wide">Адрес<input v-model="form.origin.address" name="origin-address" placeholder="Улица, дом" maxlength="255" required /></label>
                        </div>
                    </fieldset>
                    <fieldset>
                        <legend><span class="section-number">2</span> Куда доставить</legend>
                        <div class="address-fields">
                            <label>Город<input v-model="form.destination.city" name="destination-city" placeholder="Берлин" maxlength="100" required /></label>
                            <label>Страна<input v-model="form.destination.country" name="destination-country" placeholder="Германия" maxlength="100" required /></label>
                            <label class="field-wide">Адрес<input v-model="form.destination.address" name="destination-address" placeholder="Улица, дом" maxlength="255" required /></label>
                        </div>
                    </fieldset>
                </div>

                <fieldset class="parcel-fields">
                    <legend><span class="section-number">3</span> Параметры посылки</legend>
                    <div class="parcel-grid">
                        <label>Вес, кг<input v-model.number="form.weight" name="weight" type="number" min="0.001" step="0.001" required /></label>
                        <label>Длина, см<input v-model.number="form.dimensions.length" name="length" type="number" min="0.001" step="0.001" required /></label>
                        <label>Ширина, см<input v-model.number="form.dimensions.width" name="width" type="number" min="0.001" step="0.001" required /></label>
                        <label>Высота, см<input v-model.number="form.dimensions.height" name="height" type="number" min="0.001" step="0.001" required /></label>
                    </div>
                </fieldset>

                <fieldset>
                    <legend>Тип доставки</legend>
                    <div class="delivery-options">
                        <label :class="{ chosen: form.deliveryType === 'STANDARD' }"><input v-model="form.deliveryType" type="radio" value="STANDARD" name="delivery-type" /><span><strong>Стандартная</strong><small>Обычная доставка</small></span></label>
                        <label :class="{ chosen: form.deliveryType === 'EXPRESS' }"><input v-model="form.deliveryType" type="radio" value="EXPRESS" name="delivery-type" /><span><strong>Экспресс</strong><small>Приоритетная обработка</small></span></label>
                    </div>
                </fieldset>
                <p v-if="formError" class="form-error" role="alert">{{ formError }}</p>
            </div>
            <div class="dialog-footer">
                <p>Заявка сохранится на сервере и поступит в обработку.</p>
                <div><button type="button" class="button button-secondary" :disabled="submitting" @click="createDialog.close()">Отмена</button><button class="button button-primary" type="submit" :disabled="submitting">{{ submitting ? 'Создание…' : 'Создать заявку' }} <span aria-hidden="true">→</span></button></div>
            </div>
        </form>
    </dialog>
</template>
