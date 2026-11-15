package com.example.logistics.processing.repository;

import com.example.logistics.processing.entity.ProcessingStage;

/** Этап и версия, прочитанные одним запросом; это снимок для запуска worker, а не захват задачи. */
public interface TaskExecutionSnapshot {

    ProcessingStage getCurrentStage();

    long getVersion();
}
