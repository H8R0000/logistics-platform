export const shipmentStatuses = {
    CREATED: { label: 'Создана', tone: 'neutral' },
    PROCESSING: { label: 'В обработке', tone: 'purple' },
    WAITING_FOR_COURIER: { label: 'Поиск курьера', tone: 'amber' },
    IN_TRANSIT: { label: 'В пути', tone: 'blue' },
    DELIVERED: { label: 'Доставлена', tone: 'green' },
    FAILED: { label: 'Ошибка', tone: 'red' },
    COURIER_NOT_FOUND: { label: 'Курьер не найден', tone: 'red' },
}
export const taskStatuses = {
    ...shipmentStatuses,
    QUEUED: { label: 'В очереди', tone: 'neutral' },
    COMPLETED: { label: 'Завершена', tone: 'green' },
}
export const deliveryTypes = { STANDARD: 'Стандартная', EXPRESS: 'Экспресс' }
export const stages = {
    SEARCH_COURIER: 'Поиск курьера', CREATE_ROUTE: 'Создание маршрута', CONFIRM_DELIVERY: 'Подтверждение доставки',
}
export const eventTitles = {
    SHIPMENT_CREATED: 'Заявка создана', TASK_REGISTERED: 'Задача зарегистрирована',
    SHIPMENT_PROCESSING_STARTED: 'Обработка началась', COURIER_SEARCH_STARTED: 'Начат поиск курьера',
    COURIER_SEARCH_RECOVERED: 'Поиск восстановлен после перезапуска',
    WAITING_FOR_COURIER: 'Ожидание курьера', SHIPMENT_WAITING_FOR_COURIER: 'Ожидание курьера',
    COURIER_ASSIGNED: 'Курьер назначен', ROUTE_SHEET_CREATED: 'Маршрут создан',
    SHIPMENT_IN_TRANSIT: 'Доставка началась', SHIPMENT_DELIVERED: 'Доставка подтверждена',
    TASK_FAILED: 'Ошибка обработки', SHIPMENT_FAILED: 'Ошибка обработки',
    COURIER_NOT_FOUND: 'Время поиска курьера истекло',
    COURIER_SEARCH_RETRY_SCHEDULED: 'Запланирован повтор поиска',
    DELIVERY_STAGE_RETRY_SCHEDULED: 'Запланирован повтор этапа',
    TASK_RETRY_REQUESTED: 'Администратор запросил повтор', SHIPMENT_PROCESSING_RESUMED: 'Обработка возобновлена',
}
