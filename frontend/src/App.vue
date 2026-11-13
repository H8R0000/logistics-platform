<script setup>
import { computed, reactive, ref } from 'vue'
import { demoShipments, deliveryTypes, shipmentStatuses } from './mockData.js'

const shipments = ref(structuredClone(demoShipments))
const selectedId = ref(shipments.value[0].id)
const search = ref('')
const statusFilter = ref('')
const notice = ref('')
const createDialog = ref(null)
const formError = ref('')
const nextNumber = ref(1043)

function emptyForm() {
    return {
        origin: { city: '', country: '', address: '' },
        destination: { city: '', country: '', address: '' },
        weight: 1,
        dimensions: { length: 10, width: 10, height: 10 },
        deliveryType: 'STANDARD',
    }
}

const form = reactive(emptyForm())

// computed пересчитывает список, когда меняются данные, поиск или фильтр.
const filteredShipments = computed(() => {
    const query = search.value.trim().toLocaleLowerCase('ru')
    return shipments.value.filter((shipment) => {
        const text = `${shipment.number} ${shipment.origin.city} ${shipment.destination.city}`
        return (!statusFilter.value || shipment.status === statusFilter.value)
            && text.toLocaleLowerCase('ru').includes(query)
    })
})

const selectedShipment = computed(() =>
    filteredShipments.value.find((shipment) => shipment.id === selectedId.value)
        ?? filteredShipments.value[0]
        ?? null,
)

const activeCount = computed(() => shipments.value.filter((shipment) =>
    ['CREATED', 'PROCESSING', 'WAITING_FOR_COURIER', 'IN_TRANSIT'].includes(shipment.status),
).length)

const deliveredCount = computed(() =>
    shipments.value.filter((shipment) => shipment.status === 'DELIVERED').length,
)

function formatDate(value) {
    return new Intl.DateTimeFormat('ru-RU', {
        day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit',
    }).format(new Date(value))
}

function formatNumber(value) {
    return new Intl.NumberFormat('ru-RU', { maximumFractionDigits: 3 }).format(value)
}

function clearFilters() {
    search.value = ''
    statusFilter.value = ''
}

function resetDemo() {
    shipments.value = structuredClone(demoShipments)
    selectedId.value = shipments.value[0].id
    nextNumber.value = 1043
    clearFilters()
    notice.value = 'Исходные демонстрационные заявки восстановлены.'
}

function openCreateForm() {
    Object.assign(form, emptyForm())
    formError.value = ''
    createDialog.value.showModal()
}

function createShipment() {
    const origin = {
        city: form.origin.city.trim(),
        country: form.origin.country.trim(),
        address: form.origin.address.trim(),
    }
    const destination = {
        city: form.destination.city.trim(),
        country: form.destination.country.trim(),
        address: form.destination.address.trim(),
    }
    if ([...Object.values(origin), ...Object.values(destination)].some((value) => !value)) {
        formError.value = 'Заполните город, страну и адрес для обеих точек маршрута.'
        return
    }

    const now = new Date().toISOString()
    const shipment = {
        id: crypto.randomUUID(),
        number: `LG-${nextNumber.value++}`,
        origin,
        destination,
        weight: form.weight,
        dimensions: { ...form.dimensions },
        deliveryType: form.deliveryType,
        status: 'CREATED',
        createdAt: now,
        estimatedDeliveryAt: null,
        courier: null,
        events: [{ title: 'Заявка создана', at: now }],
    }

    shipments.value.unshift(shipment)
    selectedId.value = shipment.id
    clearFilters()
    createDialog.value.close()
    notice.value = `Заявка ${shipment.number} добавлена в демо-список.`
}
</script>

<template>
    <header class="topbar">
        <div class="topbar-inner">
            <a class="brand" href="#main" aria-label="Logistics — к списку заявок">
                <img src="/favicon.svg" alt="" width="40" height="40" />
                <span>Logistics<small>Платформа доставки</small></span>
            </a>
            <div class="demo-label"><span class="status-dot"></span>Демонстрационный режим</div>
        </div>
    </header>

    <main id="main" class="page">
        <section class="page-heading" aria-labelledby="page-title">
            <div>
                <p class="eyebrow">ОБЗОР ОТПРАВЛЕНИЙ</p>
                <h1 id="page-title">Заявки на доставку</h1>
                <p class="subtitle">Каждая посылка, её маршрут и история — в одном месте.</p>
            </div>
            <button class="button button-primary" @click="openCreateForm">
                <span class="plus" aria-hidden="true">+</span> Новая заявка
            </button>
        </section>

        <div v-if="notice" class="notice" role="status">
            <span><span aria-hidden="true">✓</span> {{ notice }}</span>
            <button class="icon-button" aria-label="Закрыть уведомление" @click="notice = ''">×</button>
        </div>

        <section class="metrics" aria-label="Сводка по заявкам">
            <article class="metric-card">
                <div><p class="metric-label">Всего заявок</p><p class="metric-value">{{ shipments.length }}</p></div>
                <div class="metric-symbol symbol-purple" aria-hidden="true">
                    <svg viewBox="0 0 24 24"><path d="m12 3 8 4.5v9L12 21l-8-4.5v-9Zm0 9 8-4.5M12 12 4 7.5m8 4.5v9M8 5.2l8 4.5" /></svg>
                </div>
                <p class="metric-caption">Все демонстрационные отправления</p>
            </article>
            <article class="metric-card">
                <div><p class="metric-label">Активные</p><p class="metric-value">{{ activeCount }}</p></div>
                <div class="metric-symbol symbol-blue" aria-hidden="true">
                    <svg viewBox="0 0 24 24"><path d="M3 7h11v10H3Zm11 4h4l3 4v2h-7M6 7V4h5M6 20a2 2 0 1 0 0-4 2 2 0 0 0 0 4Zm12 0a2 2 0 1 0 0-4 2 2 0 0 0 0 4Z" /></svg>
                </div>
                <p class="metric-caption">Созданы, обрабатываются или в пути</p>
            </article>
            <article class="metric-card">
                <div><p class="metric-label">Доставлены</p><p class="metric-value">{{ deliveredCount }}</p></div>
                <div class="metric-symbol symbol-green" aria-hidden="true">
                    <svg viewBox="0 0 24 24"><path d="M20 11.3V12a8 8 0 1 1-4.7-7.3M8 11l4 4 9-10" /></svg>
                </div>
                <p class="metric-caption">Доставка успешно подтверждена</p>
            </article>
        </section>

        <div class="workspace">
            <section class="panel shipments-panel" aria-labelledby="shipments-title">
                <div class="panel-heading">
                    <div class="heading-with-count">
                        <h2 id="shipments-title">Все заявки</h2><span class="count-badge">{{ shipments.length }}</span>
                    </div>
                    <span class="quiet-label">Выберите заявку для просмотра</span>
                </div>

                <div class="filters">
                    <label class="search-field">
                        <svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="10.5" cy="10.5" r="6.5" /><path d="m16 16 5 5" /></svg>
                        <input v-model="search" type="search" placeholder="Номер заявки или город" aria-label="Поиск заявок" />
                    </label>
                    <select v-model="statusFilter" aria-label="Фильтр по статусу">
                        <option value="">Все статусы</option>
                        <option v-for="(status, value) in shipmentStatuses" :key="value" :value="value">{{ status.label }}</option>
                    </select>
                </div>

                <div v-if="filteredShipments.length" class="table-scroll">
                    <table>
                        <thead><tr><th>Заявка</th><th>Маршрут</th><th>Статус</th><th class="weight-column">Вес</th></tr></thead>
                        <tbody>
                            <tr v-for="shipment in filteredShipments" :key="shipment.id"
                                :class="{ 'is-selected': selectedShipment?.id === shipment.id }"
                                @click="selectedId = shipment.id">
                                <td>
                                    <button class="shipment-link" :aria-pressed="selectedShipment?.id === shipment.id"
                                        @click.stop="selectedId = shipment.id">{{ shipment.number }}</button>
                                    <span class="cell-caption">{{ formatDate(shipment.createdAt) }}</span>
                                </td>
                                <td>
                                    <span class="route-city">{{ shipment.origin.city }} <span class="route-arrow" aria-hidden="true">→</span></span>
                                    <span class="destination-city">{{ shipment.destination.city }}</span>
                                    <span class="cell-caption">{{ deliveryTypes[shipment.deliveryType] }}</span>
                                </td>
                                <td><span class="status-badge" :class="shipmentStatuses[shipment.status].tone"><span class="status-dot"></span>{{ shipmentStatuses[shipment.status].label }}</span></td>
                                <td class="weight-column weight-value">{{ formatNumber(shipment.weight) }} <span>кг</span></td>
                            </tr>
                        </tbody>
                    </table>
                </div>
                <div v-else class="empty-state">
                    <div class="empty-symbol" aria-hidden="true">⌕</div>
                    <h3>Заявки не найдены</h3>
                    <p>Попробуйте другой город, номер или статус.</p>
                    <button class="button button-secondary" @click="clearFilters">Сбросить фильтры</button>
                </div>

                <div class="list-footer"><span>Показано {{ filteredShipments.length }} из {{ shipments.length }}</span><span class="demo-data-note"><span class="status-dot"></span>Тестовые данные</span></div>
            </section>

            <aside class="panel details-panel" aria-labelledby="details-title">
                <template v-if="selectedShipment">
                    <div class="details-heading">
                        <div><p class="eyebrow">ПОДРОБНОСТИ ЗАЯВКИ</p><h2 id="details-title">{{ selectedShipment.number }}</h2></div>
                        <span class="status-badge" :class="shipmentStatuses[selectedShipment.status].tone"><span class="status-dot"></span>{{ shipmentStatuses[selectedShipment.status].label }}</span>
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
                        <div class="fact-wide"><dt>Курьер</dt><dd :class="{ muted: !selectedShipment.courier }">{{ selectedShipment.courier ?? 'Ещё не назначен' }}</dd></div>
                        <div v-if="selectedShipment.estimatedDeliveryAt && selectedShipment.status !== 'DELIVERED'" class="fact-wide"><dt>Ожидаемая доставка</dt><dd>{{ formatDate(selectedShipment.estimatedDeliveryAt) }}</dd></div>
                    </dl>

                    <section class="history-section" aria-labelledby="history-title">
                        <h3 id="history-title">История доставки <span>{{ selectedShipment.events.length }}</span></h3>
                        <ol class="timeline">
                            <li v-for="(event, index) in selectedShipment.events" :key="`${selectedShipment.id}-${index}`"
                                :class="{ latest: index === selectedShipment.events.length - 1, completed: selectedShipment.status === 'DELIVERED' }">
                                <span class="timeline-marker" aria-hidden="true"></span>
                                <p>{{ event.title }}</p><time :datetime="event.at">{{ formatDate(event.at) }}</time>
                            </li>
                        </ol>
                    </section>
                </template>
                <div v-else class="empty-state details-empty"><h2 id="details-title">Здесь будут подробности</h2><p>Выберите заявку из списка.</p></div>
            </aside>
        </div>

        <footer class="page-footer">
            <p>Демо-макет. После обновления страницы изменения сбросятся.</p>
            <button class="text-button" @click="resetDemo">Восстановить демо-данные <span aria-hidden="true">↺</span></button>
        </footer>
    </main>

    <dialog ref="createDialog" class="create-dialog" aria-labelledby="create-title" @close="formError = ''">
        <form @submit.prevent="createShipment">
            <div class="dialog-heading">
                <div><p class="eyebrow">НОВОЕ ОТПРАВЛЕНИЕ</p><h2 id="create-title">Создать заявку</h2></div>
                <button type="button" class="icon-button" aria-label="Закрыть форму" @click="createDialog.close()">×</button>
            </div>
            <div class="dialog-body">
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
                <p>Заявка появится только в этом демо-макете.</p>
                <div><button type="button" class="button button-secondary" @click="createDialog.close()">Отмена</button><button class="button button-primary" type="submit">Создать заявку <span aria-hidden="true">→</span></button></div>
            </div>
        </form>
    </dialog>
</template>
