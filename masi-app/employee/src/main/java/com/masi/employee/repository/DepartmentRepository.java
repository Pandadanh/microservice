package com.masi.employee.repository;

import com.masi.employee.domain.Department;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Spring Data R2DBC repository for the Department entity.
 */
@SuppressWarnings("unused")
@Repository
public interface DepartmentRepository extends ReactiveCrudRepository<Department, Long>, DepartmentRepositoryInternal {
    @Query("SELECT * FROM department entity WHERE entity.location_id = :id")
    Flux<Department> findByLocation(Long id);

    @Query("SELECT * FROM department entity WHERE entity.location_id IS NULL")
    Flux<Department> findAllWhereLocationIsNull();

    @Query("SELECT * FROM department entity WHERE entity.id not in (select job_history_id from job_history)")
    Flux<Department> findAllWhereJobHistoryIsNull();

    @Override
    <S extends Department> Mono<S> save(S entity);

    @Override
    Flux<Department> findAll();

    @Override
    Mono<Department> findById(Long id);

    @Override
    Mono<Void> deleteById(Long id);
}

interface DepartmentRepositoryInternal {
    <S extends Department> Mono<S> save(S entity);

    Flux<Department> findAllBy(Pageable pageable);

    Flux<Department> findAll();

    Mono<Department> findById(Long id);
    // this is not supported at the moment because of https://github.com/jhipster/generator-jhipster/issues/18269
    // Flux<Department> findAllBy(Pageable pageable, Criteria criteria);
}
