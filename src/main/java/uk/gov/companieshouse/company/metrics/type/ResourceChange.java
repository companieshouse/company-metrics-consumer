package uk.gov.companieshouse.company.metrics.type;

import java.util.Objects;

import uk.gov.companieshouse.stream.ResourceChangedData;


public class ResourceChange {

    private final ResourceChangedData data;

    public ResourceChange(ResourceChangedData data) {
        this.data = data;
    }

    public ResourceChangedData getData() {
        return data;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ResourceChange that = (ResourceChange) obj;
        return Objects.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(data);
    }
}
