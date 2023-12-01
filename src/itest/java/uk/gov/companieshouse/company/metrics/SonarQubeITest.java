package uk.gov.companieshouse.company.metrics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SonarQubeITest {

    @Test
    void test() {
        // given
        SonarQube sonarQube = new SonarQube();

        // when
        int result = sonarQube.calculate(1,1);

        // then
        assertEquals(2, result);
    }
}
