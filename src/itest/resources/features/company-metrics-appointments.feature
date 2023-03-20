Feature: Company metrics appointment criteria

  Scenario Outline: Post officers recalculate successfully - event type changed
    Given The event type is "<eventType>"
    And A resource change data message for "<companyNumber>" with an appointment entity exists on the "<mainKafkaTopic>" kafka topic
    And Company Metrics API returns OK status code
    When The message is consumed
    Then A request is sent to the Company Metrics Recalculate endpoint

    Examples:
      | mainKafkaTopic          | eventType  | companyNumber |
      | stream-company-officers | changed    | 01203396      |
      | stream-company-officers | changed    | 08124207      |
      | stream-company-officers | changed    | SC109614      |

  Scenario Outline: Post officers recalculate successfully - event type deleted
    Given The event type is "<eventType>"
    And A resource change data message for "<companyNumber>" with an appointment entity exists on the "<mainKafkaTopic>" kafka topic
    And Company Metrics API returns OK status code
    When The message is consumed
    Then A request is sent to the Company Metrics Recalculate endpoint

    Examples:
      | mainKafkaTopic          | eventType  | companyNumber |
      | stream-company-officers | deleted    | 01203396      |
      | stream-company-officers | deleted    | 08124207      |
      | stream-company-officers | deleted    | SC109614      |

  Scenario: Invalid message on topic
    Given Company Metrics API returns OK status code
    And An invalid message exists on the "stream-company-officers" kafka topic
    When The message is consumed
    Then A non-retryable exception should be thrown when consuming from "stream-company-officers"
    And The message should be placed on to "stream-company-officers-company-metrics-consumer-invalid" kafka topic

  Scenario Outline: Service not authenticated or authorised
    Given The consumer has been configured with api key without internal app privileges for "<companyNumber>"
    And A resource change data message for "<companyNumber>" with an appointment entity exists on the "<mainKafkaTopic>" kafka topic
    When The message is consumed
    Then The message should be placed on to "stream-company-officers-company-metrics-consumer-invalid" kafka topic

    Examples:
      | mainKafkaTopic          | companyNumber |
      | stream-company-officers | 01203396      |
      | stream-company-officers | 08124207      |
      | stream-company-officers | SC109614      |

  Scenario Outline: Processing message with invalid resource URI
    Given The message resource Uri "<invalidResourceUri>" is invalid
    And A resource change data message for "<companyNumber>" with an appointment entity exists on the "<mainKafkaTopic>" kafka topic
    When The message is consumed
    Then The message should be placed on to "stream-company-officers-company-metrics-consumer-invalid" kafka topic

    Examples:
      | mainKafkaTopic          | companyNumber | invalidResourceUri                 |
      | stream-company-officers | 01203396      |                                    |
      | stream-company-officers | 08124207      | /companyabc/%s/metrics/recalculate |
      | stream-company-officers | SC109614      | abcdefg                            |


  Scenario Outline: Endpoint does not exist/could not be found in metrics api
    Given The specified endpoint does not exist within company metrics api
    And A resource change data message for "<companyNumber>" with an appointment entity exists on the "<mainKafkaTopic>" kafka topic
    When The message is consumed
    Then The message should be placed on to "stream-company-officers-company-metrics-consumer-invalid" kafka topic

    Examples:
      | mainKafkaTopic          | companyNumber |
      | stream-company-officers | 01203396      |
      | stream-company-officers | 08124207      |
      | stream-company-officers | SC109614      |

  Scenario Outline: Consume a message but company metrics api is unavailable
    Given Company Metrics API returns SERVICE_UNAVAILABLE status code
    And A resource change data message for "<companyNumber>" with an appointment entity exists on the "<mainKafkaTopic>" kafka topic
    When The message is consumed
    Then The message should be moved to topic "<kafkaErrorTopic>" after retry attempts of "<retryAttempts>"
    And Stubbed Company Metrics API should be called "<times>" times

    Examples:
      | mainKafkaTopic          | companyNumber | kafkaErrorTopic                                        | retryAttempts | times |
      | stream-company-officers | 01203396      | stream-company-officers-company-metrics-consumer-error | 4             | 4     |
      | stream-company-officers | 08124207      | stream-company-officers-company-metrics-consumer-error | 4             | 4     |
      | stream-company-officers | SC109614      | stream-company-officers-company-metrics-consumer-error | 4             | 4     |
