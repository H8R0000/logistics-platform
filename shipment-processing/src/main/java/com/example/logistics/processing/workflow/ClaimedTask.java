package com.example.logistics.processing.workflow;

import com.example.logistics.processing.entity.ShipmentData;
import java.util.UUID;

/**
 * Снимок захваченной задачи, с которым worker работает после завершения транзакции захвата.
 *
 * @param id идентификатор задачи
 * @param version версия после захвата; проверяется перед сохранением результата worker
 * @param shipmentData данные доставки для выбора курьера
 */
public record ClaimedTask(UUID id, long version, ShipmentData shipmentData) {
}
