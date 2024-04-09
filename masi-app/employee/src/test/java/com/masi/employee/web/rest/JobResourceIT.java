package com.masi.employee.web.rest;

import static com.masi.employee.domain.JobAsserts.*;
import static com.masi.employee.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.masi.employee.IntegrationTest;
import com.masi.employee.domain.Job;
import com.masi.employee.repository.EntityManager;
import com.masi.employee.repository.JobRepository;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;

/**
 * Integration tests for the {@link JobResource} REST controller.
 */
@IntegrationTest
@ExtendWith(MockitoExtension.class)
@AutoConfigureWebTestClient(timeout = IntegrationTest.DEFAULT_ENTITY_TIMEOUT)
@WithMockUser
class JobResourceIT {

    private static final String DEFAULT_JOB_TITLE = "AAAAAAAAAA";
    private static final String UPDATED_JOB_TITLE = "BBBBBBBBBB";

    private static final Long DEFAULT_MIN_SALARY = 1L;
    private static final Long UPDATED_MIN_SALARY = 2L;

    private static final Long DEFAULT_MAX_SALARY = 1L;
    private static final Long UPDATED_MAX_SALARY = 2L;

    private static final String ENTITY_API_URL = "/api/jobs";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private JobRepository jobRepository;

    @Mock
    private JobRepository jobRepositoryMock;

    @Autowired
    private EntityManager em;

    @Autowired
    private WebTestClient webTestClient;

    private Job job;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Job createEntity(EntityManager em) {
        Job job = new Job().jobTitle(DEFAULT_JOB_TITLE).minSalary(DEFAULT_MIN_SALARY).maxSalary(DEFAULT_MAX_SALARY);
        return job;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Job createUpdatedEntity(EntityManager em) {
        Job job = new Job().jobTitle(UPDATED_JOB_TITLE).minSalary(UPDATED_MIN_SALARY).maxSalary(UPDATED_MAX_SALARY);
        return job;
    }

    public static void deleteEntities(EntityManager em) {
        try {
            em.deleteAll("rel_job__task").block();
            em.deleteAll(Job.class).block();
        } catch (Exception e) {
            // It can fail, if other entities are still referring this - it will be removed later.
        }
    }

    @AfterEach
    public void cleanup() {
        deleteEntities(em);
    }

    @BeforeEach
    public void initTest() {
        deleteEntities(em);
        job = createEntity(em);
    }

    @Test
    void createJob() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Job
        var returnedJob = webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(job))
            .exchange()
            .expectStatus()
            .isCreated()
            .expectBody(Job.class)
            .returnResult()
            .getResponseBody();

        // Validate the Job in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        assertJobUpdatableFieldsEquals(returnedJob, getPersistedJob(returnedJob));
    }

    @Test
    void createJobWithExistingId() throws Exception {
        // Create the Job with an existing ID
        job.setId(1L);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(job))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Job in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    void getAllJobs() {
        // Initialize the database
        jobRepository.save(job).block();

        // Get all the jobList
        webTestClient
            .get()
            .uri(ENTITY_API_URL + "?sort=id,desc")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.[*].id")
            .value(hasItem(job.getId().intValue()))
            .jsonPath("$.[*].jobTitle")
            .value(hasItem(DEFAULT_JOB_TITLE))
            .jsonPath("$.[*].minSalary")
            .value(hasItem(DEFAULT_MIN_SALARY.intValue()))
            .jsonPath("$.[*].maxSalary")
            .value(hasItem(DEFAULT_MAX_SALARY.intValue()));
    }

    @SuppressWarnings({ "unchecked" })
    void getAllJobsWithEagerRelationshipsIsEnabled() {
        when(jobRepositoryMock.findAllWithEagerRelationships(any())).thenReturn(Flux.empty());

        webTestClient.get().uri(ENTITY_API_URL + "?eagerload=true").exchange().expectStatus().isOk();

        verify(jobRepositoryMock, times(1)).findAllWithEagerRelationships(any());
    }

    @SuppressWarnings({ "unchecked" })
    void getAllJobsWithEagerRelationshipsIsNotEnabled() {
        when(jobRepositoryMock.findAllWithEagerRelationships(any())).thenReturn(Flux.empty());

        webTestClient.get().uri(ENTITY_API_URL + "?eagerload=false").exchange().expectStatus().isOk();
        verify(jobRepositoryMock, times(1)).findAllWithEagerRelationships(any());
    }

    @Test
    void getJob() {
        // Initialize the database
        jobRepository.save(job).block();

        // Get the job
        webTestClient
            .get()
            .uri(ENTITY_API_URL_ID, job.getId())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.id")
            .value(is(job.getId().intValue()))
            .jsonPath("$.jobTitle")
            .value(is(DEFAULT_JOB_TITLE))
            .jsonPath("$.minSalary")
            .value(is(DEFAULT_MIN_SALARY.intValue()))
            .jsonPath("$.maxSalary")
            .value(is(DEFAULT_MAX_SALARY.intValue()));
    }

    @Test
    void getNonExistingJob() {
        // Get the job
        webTestClient
            .get()
            .uri(ENTITY_API_URL_ID, Long.MAX_VALUE)
            .accept(MediaType.APPLICATION_PROBLEM_JSON)
            .exchange()
            .expectStatus()
            .isNotFound();
    }

    @Test
    void putExistingJob() throws Exception {
        // Initialize the database
        jobRepository.save(job).block();

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the job
        Job updatedJob = jobRepository.findById(job.getId()).block();
        updatedJob.jobTitle(UPDATED_JOB_TITLE).minSalary(UPDATED_MIN_SALARY).maxSalary(UPDATED_MAX_SALARY);

        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, updatedJob.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(updatedJob))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the Job in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedJobToMatchAllProperties(updatedJob);
    }

    @Test
    void putNonExistingJob() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        job.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, job.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(job))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Job in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void putWithIdMismatchJob() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        job.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, longCount.incrementAndGet())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(job))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Job in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void putWithMissingIdPathParamJob() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        job.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(job))
            .exchange()
            .expectStatus()
            .isEqualTo(405);

        // Validate the Job in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void partialUpdateJobWithPatch() throws Exception {
        // Initialize the database
        jobRepository.save(job).block();

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the job using partial update
        Job partialUpdatedJob = new Job();
        partialUpdatedJob.setId(job.getId());

        partialUpdatedJob.jobTitle(UPDATED_JOB_TITLE);

        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, partialUpdatedJob.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(partialUpdatedJob))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the Job in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertJobUpdatableFieldsEquals(createUpdateProxyForBean(partialUpdatedJob, job), getPersistedJob(job));
    }

    @Test
    void fullUpdateJobWithPatch() throws Exception {
        // Initialize the database
        jobRepository.save(job).block();

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the job using partial update
        Job partialUpdatedJob = new Job();
        partialUpdatedJob.setId(job.getId());

        partialUpdatedJob.jobTitle(UPDATED_JOB_TITLE).minSalary(UPDATED_MIN_SALARY).maxSalary(UPDATED_MAX_SALARY);

        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, partialUpdatedJob.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(partialUpdatedJob))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the Job in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertJobUpdatableFieldsEquals(partialUpdatedJob, getPersistedJob(partialUpdatedJob));
    }

    @Test
    void patchNonExistingJob() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        job.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, job.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(job))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Job in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void patchWithIdMismatchJob() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        job.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, longCount.incrementAndGet())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(job))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Job in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void patchWithMissingIdPathParamJob() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        job.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(job))
            .exchange()
            .expectStatus()
            .isEqualTo(405);

        // Validate the Job in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void deleteJob() {
        // Initialize the database
        jobRepository.save(job).block();

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the job
        webTestClient
            .delete()
            .uri(ENTITY_API_URL_ID, job.getId())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNoContent();

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return jobRepository.count().block();
    }

    protected void assertIncrementedRepositoryCount(long countBefore) {
        assertThat(countBefore + 1).isEqualTo(getRepositoryCount());
    }

    protected void assertDecrementedRepositoryCount(long countBefore) {
        assertThat(countBefore - 1).isEqualTo(getRepositoryCount());
    }

    protected void assertSameRepositoryCount(long countBefore) {
        assertThat(countBefore).isEqualTo(getRepositoryCount());
    }

    protected Job getPersistedJob(Job job) {
        return jobRepository.findById(job.getId()).block();
    }

    protected void assertPersistedJobToMatchAllProperties(Job expectedJob) {
        // Test fails because reactive api returns an empty object instead of null
        // assertJobAllPropertiesEquals(expectedJob, getPersistedJob(expectedJob));
        assertJobUpdatableFieldsEquals(expectedJob, getPersistedJob(expectedJob));
    }

    protected void assertPersistedJobToMatchUpdatableProperties(Job expectedJob) {
        // Test fails because reactive api returns an empty object instead of null
        // assertJobAllUpdatablePropertiesEquals(expectedJob, getPersistedJob(expectedJob));
        assertJobUpdatableFieldsEquals(expectedJob, getPersistedJob(expectedJob));
    }
}
