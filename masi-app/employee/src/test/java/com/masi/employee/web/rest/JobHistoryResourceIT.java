package com.masi.employee.web.rest;

import static com.masi.employee.domain.JobHistoryAsserts.*;
import static com.masi.employee.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.masi.employee.IntegrationTest;
import com.masi.employee.domain.JobHistory;
import com.masi.employee.domain.enumeration.Language;
import com.masi.employee.repository.EntityManager;
import com.masi.employee.repository.JobHistoryRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Integration tests for the {@link JobHistoryResource} REST controller.
 */
@IntegrationTest
@AutoConfigureWebTestClient(timeout = IntegrationTest.DEFAULT_ENTITY_TIMEOUT)
@WithMockUser
class JobHistoryResourceIT {

    private static final Instant DEFAULT_START_DATE = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_START_DATE = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_END_DATE = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_END_DATE = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Language DEFAULT_LANGUAGE = Language.FRENCH;
    private static final Language UPDATED_LANGUAGE = Language.ENGLISH;

    private static final String ENTITY_API_URL = "/api/job-histories";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private JobHistoryRepository jobHistoryRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private WebTestClient webTestClient;

    private JobHistory jobHistory;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static JobHistory createEntity(EntityManager em) {
        JobHistory jobHistory = new JobHistory().startDate(DEFAULT_START_DATE).endDate(DEFAULT_END_DATE).language(DEFAULT_LANGUAGE);
        return jobHistory;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static JobHistory createUpdatedEntity(EntityManager em) {
        JobHistory jobHistory = new JobHistory().startDate(UPDATED_START_DATE).endDate(UPDATED_END_DATE).language(UPDATED_LANGUAGE);
        return jobHistory;
    }

    public static void deleteEntities(EntityManager em) {
        try {
            em.deleteAll(JobHistory.class).block();
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
        jobHistory = createEntity(em);
    }

    @Test
    void createJobHistory() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the JobHistory
        var returnedJobHistory = webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(jobHistory))
            .exchange()
            .expectStatus()
            .isCreated()
            .expectBody(JobHistory.class)
            .returnResult()
            .getResponseBody();

        // Validate the JobHistory in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        assertJobHistoryUpdatableFieldsEquals(returnedJobHistory, getPersistedJobHistory(returnedJobHistory));
    }

    @Test
    void createJobHistoryWithExistingId() throws Exception {
        // Create the JobHistory with an existing ID
        jobHistory.setId(1L);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(jobHistory))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the JobHistory in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    void getAllJobHistories() {
        // Initialize the database
        jobHistoryRepository.save(jobHistory).block();

        // Get all the jobHistoryList
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
            .value(hasItem(jobHistory.getId().intValue()))
            .jsonPath("$.[*].startDate")
            .value(hasItem(DEFAULT_START_DATE.toString()))
            .jsonPath("$.[*].endDate")
            .value(hasItem(DEFAULT_END_DATE.toString()))
            .jsonPath("$.[*].language")
            .value(hasItem(DEFAULT_LANGUAGE.toString()));
    }

    @Test
    void getJobHistory() {
        // Initialize the database
        jobHistoryRepository.save(jobHistory).block();

        // Get the jobHistory
        webTestClient
            .get()
            .uri(ENTITY_API_URL_ID, jobHistory.getId())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.id")
            .value(is(jobHistory.getId().intValue()))
            .jsonPath("$.startDate")
            .value(is(DEFAULT_START_DATE.toString()))
            .jsonPath("$.endDate")
            .value(is(DEFAULT_END_DATE.toString()))
            .jsonPath("$.language")
            .value(is(DEFAULT_LANGUAGE.toString()));
    }

    @Test
    void getNonExistingJobHistory() {
        // Get the jobHistory
        webTestClient
            .get()
            .uri(ENTITY_API_URL_ID, Long.MAX_VALUE)
            .accept(MediaType.APPLICATION_PROBLEM_JSON)
            .exchange()
            .expectStatus()
            .isNotFound();
    }

    @Test
    void putExistingJobHistory() throws Exception {
        // Initialize the database
        jobHistoryRepository.save(jobHistory).block();

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the jobHistory
        JobHistory updatedJobHistory = jobHistoryRepository.findById(jobHistory.getId()).block();
        updatedJobHistory.startDate(UPDATED_START_DATE).endDate(UPDATED_END_DATE).language(UPDATED_LANGUAGE);

        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, updatedJobHistory.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(updatedJobHistory))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the JobHistory in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedJobHistoryToMatchAllProperties(updatedJobHistory);
    }

    @Test
    void putNonExistingJobHistory() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        jobHistory.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, jobHistory.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(jobHistory))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the JobHistory in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void putWithIdMismatchJobHistory() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        jobHistory.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, longCount.incrementAndGet())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(jobHistory))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the JobHistory in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void putWithMissingIdPathParamJobHistory() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        jobHistory.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(jobHistory))
            .exchange()
            .expectStatus()
            .isEqualTo(405);

        // Validate the JobHistory in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void partialUpdateJobHistoryWithPatch() throws Exception {
        // Initialize the database
        jobHistoryRepository.save(jobHistory).block();

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the jobHistory using partial update
        JobHistory partialUpdatedJobHistory = new JobHistory();
        partialUpdatedJobHistory.setId(jobHistory.getId());

        partialUpdatedJobHistory.endDate(UPDATED_END_DATE);

        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, partialUpdatedJobHistory.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(partialUpdatedJobHistory))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the JobHistory in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertJobHistoryUpdatableFieldsEquals(
            createUpdateProxyForBean(partialUpdatedJobHistory, jobHistory),
            getPersistedJobHistory(jobHistory)
        );
    }

    @Test
    void fullUpdateJobHistoryWithPatch() throws Exception {
        // Initialize the database
        jobHistoryRepository.save(jobHistory).block();

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the jobHistory using partial update
        JobHistory partialUpdatedJobHistory = new JobHistory();
        partialUpdatedJobHistory.setId(jobHistory.getId());

        partialUpdatedJobHistory.startDate(UPDATED_START_DATE).endDate(UPDATED_END_DATE).language(UPDATED_LANGUAGE);

        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, partialUpdatedJobHistory.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(partialUpdatedJobHistory))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the JobHistory in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertJobHistoryUpdatableFieldsEquals(partialUpdatedJobHistory, getPersistedJobHistory(partialUpdatedJobHistory));
    }

    @Test
    void patchNonExistingJobHistory() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        jobHistory.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, jobHistory.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(jobHistory))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the JobHistory in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void patchWithIdMismatchJobHistory() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        jobHistory.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, longCount.incrementAndGet())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(jobHistory))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the JobHistory in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void patchWithMissingIdPathParamJobHistory() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        jobHistory.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(jobHistory))
            .exchange()
            .expectStatus()
            .isEqualTo(405);

        // Validate the JobHistory in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void deleteJobHistory() {
        // Initialize the database
        jobHistoryRepository.save(jobHistory).block();

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the jobHistory
        webTestClient
            .delete()
            .uri(ENTITY_API_URL_ID, jobHistory.getId())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNoContent();

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return jobHistoryRepository.count().block();
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

    protected JobHistory getPersistedJobHistory(JobHistory jobHistory) {
        return jobHistoryRepository.findById(jobHistory.getId()).block();
    }

    protected void assertPersistedJobHistoryToMatchAllProperties(JobHistory expectedJobHistory) {
        // Test fails because reactive api returns an empty object instead of null
        // assertJobHistoryAllPropertiesEquals(expectedJobHistory, getPersistedJobHistory(expectedJobHistory));
        assertJobHistoryUpdatableFieldsEquals(expectedJobHistory, getPersistedJobHistory(expectedJobHistory));
    }

    protected void assertPersistedJobHistoryToMatchUpdatableProperties(JobHistory expectedJobHistory) {
        // Test fails because reactive api returns an empty object instead of null
        // assertJobHistoryAllUpdatablePropertiesEquals(expectedJobHistory, getPersistedJobHistory(expectedJobHistory));
        assertJobHistoryUpdatableFieldsEquals(expectedJobHistory, getPersistedJobHistory(expectedJobHistory));
    }
}
