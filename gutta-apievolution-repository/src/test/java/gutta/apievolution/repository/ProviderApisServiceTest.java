package gutta.apievolution.repository;

import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class ProviderApisServiceTest {

    @Test
    void createNewRevision() {
        final String testHistory = "test";
        final String testApiDefinition = "api test {}";

        SimpleApisRepositoryMock apisRepositoryMock = new SimpleApisRepositoryMock();

        ProviderApisService service = new ProviderApisService();
        service.apisRepository = apisRepositoryMock;

        service.saveApiRevision(testHistory, testApiDefinition);

        PersistentProviderApiDefinition definition = apisRepositoryMock.findByRevision(testHistory, 0)
                .orElseThrow(NoSuchElementException::new);

        assertEquals(testApiDefinition, definition.getDefinitionText());
    }

    @Test
    void addRevisionToHistory() {
        final String testHistory = "test";
        final String testApiDefinition1 = "api test { record A {string fieldA}}";
        final String testApiDefinition2 = "api test { record A {string fieldB replaces fieldA}}";

        SimpleApisRepositoryMock apisRepositoryMock = new SimpleApisRepositoryMock();

        ProviderApisService service = new ProviderApisService();
        service.apisRepository = apisRepositoryMock;

        service.saveApiRevision(testHistory, testApiDefinition1);
        service.saveApiRevision(testHistory, testApiDefinition2);

        PersistentProviderApiDefinition revision1 = apisRepositoryMock.findByRevision(testHistory, 0)
                .orElseThrow(NoSuchElementException::new);
        PersistentProviderApiDefinition revision2 = apisRepositoryMock.findByRevision(testHistory, 1)
                .orElseThrow(NoSuchElementException::new);

        assertEquals(testApiDefinition1, revision1.getDefinitionText());
    }

    @Test
    void invalidHistory() {
        final String testHistory = "test";
        final String testApiDefinition1 = "api test { record A {}}";
        final String testApiDefinition2 = "api test { record A {string fieldB replaces fieldX}}";

        SimpleApisRepositoryMock apisRepositoryMock = new SimpleApisRepositoryMock();

        ProviderApisService service = new ProviderApisService();
        service.apisRepository = apisRepositoryMock;

        service.saveApiRevision(testHistory, testApiDefinition1);
        ApiProcessingException exception = assertThrows(ApiProcessingException.class,
                () -> service.saveApiRevision(testHistory, testApiDefinition2));

        // Make sure that we got the expected message
        assertTrue(exception.getMessage().contains("No predecessor field"));
    }

    /**
     * A simple repository mocks for up to 10 definitions per history (due to the naming scheme).
     */
    private static class SimpleApisRepositoryMock extends ProviderApisRepository {

        private Map<String, PersistentProviderApiDefinition> definitionMap = new HashMap<>();

        @Override
        public List<PersistentProviderApiDefinition> findApiDefinitionsInHistory(String historyName) {
            String keyPrefix = historyName + "_";

            List<String> keys = this.definitionMap.keySet().stream()
                    .filter(name -> name.startsWith(keyPrefix))
                    .collect(Collectors.toList());

            Collections.sort(keys);

            return keys.stream()
                    .map(this.definitionMap::get)
                    .collect(Collectors.toList());
        }

        @Override
        public Optional<PersistentProviderApiDefinition> findByRevision(String historyName, int revisionNumber) {
            String key = historyName + "_" + revisionNumber;
            return Optional.ofNullable(this.definitionMap.get(key));
        }

        @Override
        public void saveDefinition(PersistentProviderApiDefinition definition) {
            String key = definition.getHistoryName() + "_" + definition.getRevisionNumber();
            this.definitionMap.put(key, definition);
        }
    }

}
