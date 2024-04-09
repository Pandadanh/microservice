package com.masi.employee.service.impl;

import com.masi.employee.domain.Region;
import com.masi.employee.repository.RegionRepository;
import com.masi.employee.service.RegionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service Implementation for managing {@link com.masi.employee.domain.Region}.
 */
@Service
@Transactional
public class RegionServiceImpl implements RegionService {

    private final Logger log = LoggerFactory.getLogger(RegionServiceImpl.class);

    private final RegionRepository regionRepository;

    public RegionServiceImpl(RegionRepository regionRepository) {
        this.regionRepository = regionRepository;
    }

    @Override
    public Mono<Region> save(Region region) {
        log.debug("Request to save Region : {}", region);
        return regionRepository.save(region);
    }

    @Override
    public Mono<Region> update(Region region) {
        log.debug("Request to update Region : {}", region);
        return regionRepository.save(region);
    }

    @Override
    public Mono<Region> partialUpdate(Region region) {
        log.debug("Request to partially update Region : {}", region);

        return regionRepository
            .findById(region.getId())
            .map(existingRegion -> {
                if (region.getRegionName() != null) {
                    existingRegion.setRegionName(region.getRegionName());
                }

                return existingRegion;
            })
            .flatMap(regionRepository::save);
    }

    @Override
    @Transactional(readOnly = true)
    public Flux<Region> findAll() {
        log.debug("Request to get all Regions");
        return regionRepository.findAll();
    }

    /**
     *  Get all the regions where Country is {@code null}.
     *  @return the list of entities.
     */
    @Transactional(readOnly = true)
    public Flux<Region> findAllWhereCountryIsNull() {
        log.debug("Request to get all regions where Country is null");
        return regionRepository.findAllWhereCountryIsNull();
    }

    public Mono<Long> countAll() {
        return regionRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public Mono<Region> findOne(Long id) {
        log.debug("Request to get Region : {}", id);
        return regionRepository.findById(id);
    }

    @Override
    public Mono<Void> delete(Long id) {
        log.debug("Request to delete Region : {}", id);
        return regionRepository.deleteById(id);
    }
}
