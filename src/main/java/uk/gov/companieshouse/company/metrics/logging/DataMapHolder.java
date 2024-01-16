package uk.gov.companieshouse.company.metrics.logging;

import java.util.Map;
import uk.gov.companieshouse.logging.util.DataMap.Builder;

public class DataMapHolder {

    private static final ThreadLocal<Builder> DATAMAP_BUILDER
            = ThreadLocal.withInitial(() -> new Builder().requestId("uninitialised"));

    public static void initialise(String requestId) {
        DATAMAP_BUILDER.get().requestId(requestId);
    }

    private DataMapHolder() {
    }

    public static void clear() {
        DATAMAP_BUILDER.remove();
    }

    public static Builder get() {
        return DATAMAP_BUILDER.get();
    }

    /**
     * Populates and returns a Map of the current DataMap values.
     * @return Populated log map
     */
    public static Map<String, Object> getLogMap() {
        return DATAMAP_BUILDER.get()
                .build()
                .getLogMap();
    }

    public static String getRequestId() {
        return (String) getLogMap().get("request_id");
    }
}
