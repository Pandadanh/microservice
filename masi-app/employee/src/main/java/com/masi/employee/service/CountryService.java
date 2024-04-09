package com.masi.employee.service;

import com.masi.employee.domain.Country;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service Interface for managing {@link com.masi.employee.domain.Country}.
 */
public interface CountryService {
    /**
     * Save a country.
     *
     * @param country the entity to save.
     * @return the persisted entity.
     */
    Mono<Country> save(Country country);

    /**
     * Updates a country.
     *
     * @param country the entity to update.
     * @return the persisted entity.
     */
    Mono<Country> update(Country country);

    /**
     * Partially updates a country.
     *
     * @param country the entity to update partially.
     * @return the persisted entity.
     */
    Mono<Country> partialUpdate(Country country);

    /**
     * Get all the countries.
     *
     * @return the list of entities.
     */
    Flux<Country> findAll();

    /**
     * Get all the Country where Location is {@code null}.
     *
     * @return the {@link Flux} of entities.
     */
    Flux<Country> findAllWhereLocationIsNull();

    /**
     * Returns the number of countries available.
     * @return the number of entities in the database.
     *
     */
    Mono<Long> countAll();

    /**
     * Get the "id" country.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Mono<Country> findOne(Long id);

    /**
     * Delete the "id" country.
     *
     * @param id the id of the entity.
     * @return a Mono to signal the deletion
     */
    Mono<Void> delete(Long id);
}
