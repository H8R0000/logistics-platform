package com.example.logistics.processing.repository;

import com.example.logistics.processing.entity.ProcessingTaskStatus;

/** Одна строка результата GROUP BY status; отдельной таблицы у этой проекции нет. */
public interface TaskStatusCount {

    ProcessingTaskStatus getStatus();

    long getCount();
}
