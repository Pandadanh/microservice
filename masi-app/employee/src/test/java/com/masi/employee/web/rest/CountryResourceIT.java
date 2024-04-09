package com.masi.employee.web.rest;

import static com.masi.employee.domain.CountryAsserts.*;
import static com.masi.employee.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.masi.employee.IntegrationTest;
import com.masi.employee.domain.Country;
import com.masi.employee.repository.CountryRepository;
import com.masi.employee.repository.EntityManager;
import java.time.Duration;
import java.util.List;
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
 * Integration tests for the {@link CountryResource} REST controller.
 */
@IntegrationTest
@AutoConfigureWebTestClient(timeout = IntegrationTest.DEFAULT_ENTITY_TIMEOUT)
@WithMockUser
class CountryResourceIT {

    private static final String DEFAULT_COUNTRY_NAME = "AAAAAAAAAA";
    private static final String UPDATED_COUNTRY_NAME = "BBBBBBBBBB";

    private static final String ENTITY_API_URL = "/api/countries";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private CountryRepository countryRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private WebTestClient webTestClient;

    private Country country;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Country createEntity(EntityManager em) {
        Country country = new Country().countryName(DEFAULT_COUNTRY_NAME);
        return country;
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Country createUpdatedEntity(EntityManager em) {
        Country country = new Country().countryName(UPDATED_COUNTRY_NAME);
        return country;
    }

    public static void deleteEntities(EntityManager em) {
        try {
            em.deleteAll(Country.class).block();
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
        country = createEntity(em);
    }

    @Test
    void createCountry() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Country
        var returnedCountry = webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(country))
            .exchange()
            .expectStatus()
            .isCreated()
            .expectBody(Country.class)
            .returnResult()
            .getResponseBody();

        // Validate the Country in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        assertCountryUpdatableFieldsEquals(returnedCountry, getPersistedCountry(returnedCountry));
    }

    @Test
    void createCountryWithExistingId() throws Exception {
        // Create the Country with an existing ID
        country.setId(1L);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(country))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Country in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    void getAllCountriesAsStream() {
        // Initialize the database
        countryRepository.save(country).block();

        List<Country> countryList = webTestClient
            .get()
            .uri(ENTITY_API_URL)
            .accept(MediaType.APPLICATION_NDJSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentTypeCompatibleWith(MediaType.APPLICATION_NDJSON)
            .returnResult(Country.class)
            .getResponseBody()
            .filter(country::equals)
            .collectList()
            .block(Duration.ofSeconds(5));

        assertThat(countryList).isNotNull();
        assertThat(countryList).hasSize(1);
        Country testCountry = countryList.get(0);

        // Test fails because reactive api returns an empty object instead of null
        // assertCountryAllPropertiesEquals(country, testCountry);
        assertCountryUpdatableFieldsEquals(country, testCountry);
    }

    @Test
    void getAllCountries() {
        // Initialize the database
        countryRepository.save(country).block();

        // Get all the countryList
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
            .value(hasItem(country.getId().intValue()))
            .jsonPath("$.[*].countryName")
            .value(hasItem(DEFAULT_COUNTRY_NAME));
    }

    @Test
    void getCountry() {
        // Initialize the database
        countryRepository.save(country).block();

        // Get the country
        webTestClient
            .get()
            .uri(ENTITY_API_URL_ID, country.getId())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.id")
            .value(is(country.getId().intValue()))
            .jsonPath("$.countryName")
            .value(is(DEFAULT_COUNTRY_NAME));
    }

    @Test
    void getNonExistingCountry() {
        // Get the country
        webTestClient
            .get()
            .uri(ENTITY_API_URL_ID, Long.MAX_VALUE)
            .accept(MediaType.APPLICATION_PROBLEM_JSON)
            .exchange()
            .expectStatus()
            .isNotFound();
    }

    @Test
    void putExistingCountry() throws Exception {
        // Initialize the database
        countryRepository.save(country).block();

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the country
        Country updatedCountry = countryRepository.findById(country.getId()).block();
        updatedCountry.countryName(UPDATED_COUNTRY_NAME);

        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, updatedCountry.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(updatedCountry))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the Country in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedCountryToMatchAllProperties(updatedCountry);
    }

    @Test
    void putNonExistingCountry() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        country.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, country.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(country))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Country in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void putWithIdMismatchCountry() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        country.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, longCount.incrementAndGet())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(country))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Country in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void putWithMissingIdPathParamCountry() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        country.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(country))
            .exchange()
            .expectStatus()
            .isEqualTo(405);

        // Validate the Country in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void partialUpdateCountryWithPatch() throws Exception {
        // Initialize the database
        countryRepository.save(country).block();

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the country using partial update
        Country partialUpdatedCountry = new Country();
        partialUpdatedCountry.setId(country.getId());

        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, partialUpdatedCountry.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(partialUpdatedCountry))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the Country in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertCountryUpdatableFieldsEquals(createUpdateProxyForBean(partialUpdatedCountry, country), getPersistedCountry(country));
    }

    @Test
    void fullUpdateCountryWithPatch() throws Exception {
        // Initialize the database
        countryRepository.save(country).block();

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the country using partial update
        Country partialUpdatedCountry = new Country();
        partialUpdatedCountry.setId(country.getId());

        partialUpdatedCountry.countryName(UPDATED_COUNTRY_NAME);

        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, partialUpdatedCountry.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(partialUpdatedCountry))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the Country in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertCountryUpdatableFieldsEquals(partialUpdatedCountry, getPersistedCountry(partialUpdatedCountry));
    }

    @Test
    void patchNonExistingCountry() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        country.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, country.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(country))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Country in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void patchWithIdMismatchCountry() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        country.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, longCount.incrementAndGet())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(country))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Country in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void patchWithMissingIdPathParamCountry() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        country.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(country))
            .exchange()
            .expectStatus()
            .isEqualTo(405);

        // Validate the Country in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void deleteCountry() {
        // Initialize the database
        countryRepository.save(country).block();

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the country
        webTestClient
            .delete()
            .uri(ENTITY_API_URL_ID, country.getId())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNoContent();

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return countryRepository.count().block();
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

    protected Country getPersistedCountry(Country country) {
        return countryRepository.findById(country.getId()).block();
    }

    protected void assertPersistedCountryToMatchAllProperties(Country expectedCountry) {
        // Test fails because reactive api returns an empty object instead of null
        // assertCountryAllPropertiesEquals(expectedCountry, getPersistedCountry(expectedCountry));
        assertCountryUpdatableFieldsEquals(expectedCountry, getPersistedCountry(expectedCountry));
    }

    protected void assertPersistedCountryToMatchUpdatableProperties(Country expectedCountry) {
        // Test fails because reactive api returns an empty object instead of null
        // assertCountryAllUpdatablePropertiesEquals(expectedCountry, getPersistedCountry(expectedCountry));
        assertCountryUpdatableFieldsEquals(expectedCountry, getPersistedCountry(expectedCountry));
    }
}
