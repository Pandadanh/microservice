package com.masi.employee.service;

import com.masi.employee.domain.Task;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service Interface for managing {@link com.masi.employee.domain.Task}.
 */
public interface TaskService {
    /**
     * Save a task.
     *
     * @param task the entity to save.
     * @return the persisted entity.
     */
    Mono<Task> save(Task task);

    /**
     * Updates a task.
     *
     * @param task the entity to update.
     * @return the persisted entity.
     */
    Mono<Task> update(Task task);

    /**
     * Partially updates a task.
     *
     * @param task the entity to update partially.
     * @return the persisted entity.
     */
    Mono<Task> partialUpdate(Task task);

    /**
     * Get all the tasks.
     *
     * @return the list of entities.
     */
    Flux<Task> findAll();

    /**
     * Returns the number of tasks available.
     * @return the number of entities in the database.
     *
     */
    Mono<Long> countAll();

    /**
     * Get the "id" task.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Mono<Task> findOne(Long id);

    /**
     * Delete the "id" task.
     *
     * @param id the id of the entity.
     * @return a Mono to signal the deletion
     */
    Mono<Void> delete(Long id);
}
