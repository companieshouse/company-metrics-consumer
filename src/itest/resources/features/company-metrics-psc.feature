Feature: Company metrics PSC criteria

  Scenario: Invalid message on topic
    Given Company Metrics API returns OK status code
    And An invalid message exists on the "stream-company-psc" kafka topic
    When The message is consumed
    Then A non-retryable exception should be thrown when consuming from "stream-company-psc"
    And The message should be placed on to "stream-company-psc-company-metrics-consumer-invalid" kafka topic


  Scenario Outline: Post psc recalculate successfully - event type changed
    Given The event type is "<eventType>"
    And Company Metrics API returns OK status code
    And A resource change data message for "<companyNumber>" with an psc entity exists on the "<mainKafkaTopic>" kafka topic
    When The message is consumed
    Then A request is sent to the Company Metrics Recalculate endpoint for PSCs

    Examples:
      | mainKafkaTopic     | eventType  | companyNumber |
      | stream-company-psc | changed    | 01203396      |
      | stream-company-psc | changed    | 08124207      |
      | stream-company-psc | changed    | SC109614      |

  Scenario Outline: Post psc recalculate successfully - event type deleted
    Given The event type is "<eventType>"
    And Company Metrics API returns OK status code
    And A resource change data message for "<companyNumber>" with an psc entity exists on the "<mainKafkaTopic>" kafka topic
    When The message is consumed
    Then A request is sent to the Company Metrics Recalculate endpoint for PSCs

    Examples:
      | mainKafkaTopic     | eventType  | companyNumber |
      | stream-company-psc | deleted    | 01203396      |
      | stream-company-psc | deleted    | 08124207      |
      | stream-company-psc | deleted    | SC109614      |

  Scenario Outline: Metrics Api returns 400 bad request
    Given Company Metrics API returns BAD_REQUEST status code
    And A resource change data message for "<companyNumber>" with an appointment entity exists on the "<mainKafkaTopic>" kafka topic
    When The message is consumed
    Then The message should be placed on to "stream-company-psc-company-metrics-consumer-invalid" kafka topic

    Examples:
      | mainKafkaTopic          | companyNumber |
      | stream-company-psc | 01203396      |
      | stream-company-psc | 08124207      |
      | stream-company-psc | SC109614      |

  Scenario Outline: Processing message with invalid resource URI
    Given The message resource Uri "<invalidResourceUri>" is invalid
    And A resource change data message for "<companyNumber>" with an appointment entity exists on the "<mainKafkaTopic>" kafka topic
    When The message is consumed
    Then The message should be placed on to "stream-company-psc-company-metrics-consumer-invalid" kafka topic

    Examples:
      | mainKafkaTopic          | companyNumber | invalidResourceUri            |
      | stream-company-psc | 01203396      |                                    |
      | stream-company-psc | 08124207      | /companyabc/%s/metrics/recalculate |
      | stream-company-psc | SC109614      | abcdefg                            |

  Scenario Outline: Metrics Api returns 409 conflict
    Given Company Metrics API returns CONFLICT status code
    And A resource change data message for "<companyNumber>" with an appointment entity exists on the "<mainKafkaTopic>" kafka topic
    When The message is consumed
    Then The message should be placed on to "stream-company-psc-company-metrics-consumer-invalid" kafka topic

    Examples:
      | mainKafkaTopic     | companyNumber |
      | stream-company-psc | 01203396      |
      | stream-company-psc | 08124207      |
      | stream-company-psc | SC109614      |

  Scenario Outline: Consume a message but company metrics api is unavailable
    Given Company Metrics API returns SERVICE_UNAVAILABLE status code
    And A resource change data message for "<companyNumber>" with an appointment entity exists on the "<mainKafkaTopic>" kafka topic
    When The message is consumed
    Then The message should be moved to topic "<kafkaErrorTopic>" after retry attempts of "<retryAttempts>"
    And Stubbed Company Metrics API should be called "<times>" times

    Examples:
      | mainKafkaTopic     | companyNumber | kafkaErrorTopic                                   | retryAttempts | times |
      | stream-company-psc | 01203396      | stream-company-psc-company-metrics-consumer-error | 4             | 4     |
      | stream-company-psc | 08124207      | stream-company-psc-company-metrics-consumer-error | 4             | 4     |
      | stream-company-psc | SC109614      | stream-company-psc-company-metrics-consumer-error | 4             | 4     |
