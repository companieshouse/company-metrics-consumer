Feature: Company metrics PSC criteria

  Scenario: Invalid message on topic
    Given Company Metrics API returns OK status code
    And An invalid message exists on the "stream-company-psc" kafka topic
    When The message is consumed
    Then A non-retryable exception should be thrown when consuming from "stream-company-psc"
    And The message should be placed on to "stream-company-psc-company-metrics-consumer-invalid" kafka topic


  Scenario Outline: Post psc recalculate successfully - event type changed
    Given The event type is "<eventType>"
    And A resource change data message for "<companyNumber>" with an psc entity exists on the "<mainKafkaTopic>" kafka topic
    And Company Metrics API returns OK status code
    When The message is consumed
    Then A request is sent to the Company Metrics Recalculate endpoint

    Examples:
      | mainKafkaTopic     | eventType  | companyNumber |
      | stream-company-psc | changed    | 01203396      |
      | stream-company-psc | changed    | 08124207      |
      | stream-company-psc | changed    | SC109614      |