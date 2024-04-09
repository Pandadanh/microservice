package com.masi.employee.service;

import com.masi.employee.domain.Region;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service Interface for managing {@link com.masi.employee.domain.Region}.
 */
public interface RegionService {
    /**
     * Save a region.
     *
     * @param region the entity to save.
     * @return the persisted entity.
     */
    Mono<Region> save(Region region);

    /**
     * Updates a region.
     *
     * @param region the entity to update.
     * @return the persisted entity.
     */
    Mono<Region> update(Region region);

    /**
     * Partially updates a region.
     *
     * @param region the entity to update partially.
     * @return the persisted entity.
     */
    Mono<Region> partialUpdate(Region region);

    /**
     * Get all the regions.
     *
     * @return the list of entities.
     */
    Flux<Region> findAll();

    /**
     * Get all the Region where Country is {@code null}.
     *
     * @return the {@link Flux} of entities.
     */
    Flux<Region> findAllWhereCountryIsNull();

    /**
     * Returns the number of regions available.
     * @return the number of entities in the database.
     *
     */
    Mono<Long> countAll();

    /**
     * Get the "id" region.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Mono<Region> findOne(Long id);

    /**
     * Delete the "id" region.
     *
     * @param id the id of the entity.
     * @return a Mono to signal the deletion
     */
    Mono<Void> delete(Long id);
}
