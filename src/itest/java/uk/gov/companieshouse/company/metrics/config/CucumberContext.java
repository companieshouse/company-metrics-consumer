package uk.gov.companieshouse.company.metrics.config;

import java.util.HashMap;
import java.util.Map;

/**
 * Context to store the state.
 */
public enum CucumberContext {

    CONTEXT;

    private final ThreadLocal<Map<String, Object>> testContexts = ThreadLocal.withInitial(
            HashMap::new);

    public Object get(String name) {
        return testContexts.get()
                .get(name);
    }

    public <T> T set(String name, T object) {
        testContexts.get()
                .put(name, object);
        return object;
    }

    public void clear() {
        testContexts.get()
                .clear();
    }
}
