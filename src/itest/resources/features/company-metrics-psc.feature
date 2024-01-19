Feature: Company metrics PSC criteria

  Scenario: Invalid message on topic
    Given Company Metrics API returns OK status code
    And An invalid message exists on the "stream-company-psc" kafka topic
    When The message is consumed
    Then A non-retryable exception should be thrown when consuming from "stream-company-psc"
    And The message should be placed on to "stream-company-psc-company-metrics-consumer-invalid" kafka topic