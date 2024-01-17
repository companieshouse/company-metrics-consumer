Feature: Process company metrics charges-stream error and retry scenarios

  Scenario Outline: Processing invalid message

    Given Company Metrics API returns OK status code
    When An invalid message "<invalidMessage>" is sent to the Kafka topic "<KafkaTopic>"
    Then The message should be moved to topic "<KafkaInvalidTopic>" after retry attempts of "<retryAttempts>"
    And Stubbed Company Metrics API should be called "<times>" times

    Examples:
      | invalidMessage      | KafkaTopic             | KafkaInvalidTopic                                       | times | retryAttempts |
      | non_avro_msg_1.avro | stream-company-charges | stream-company-charges-company-metrics-consumer-invalid | 0     | 0             |

  Scenario Outline: Processing message with invalid resource uri

    Given Company Metrics API returns OK status code
    And Charges Data API returns OK status code for relevant "<companyNumber>"
    When A message with invalid resourceURI for "<companyNumber>" is sent to the Kafka topic "<KafkaTopic>"
    Then The message should be moved to topic "<KafkaInvalidTopic>" after retry attempts of "<retryAttempts>"
    And Stubbed Company Metrics API should be called "<times>" times

    Examples:
      | companyNumber | KafkaTopic             | KafkaInvalidTopic                                       | times | retryAttempts |
      | 12345678      | stream-company-charges | stream-company-charges-company-metrics-consumer-invalid | 0     | 0             |

  Scenario Outline: Processing message and metrics api returning bad request error

    Given Company Metrics API returns BAD_REQUEST status code
    And  Charges Data API returns OK status code for relevant "<companyNumber>"
    When A message for "<companyNumber>" and changed eventType is successfully sent to the Kafka topic "<KafkaTopic>"
    Then The message should be moved to topic "<KafkaInvalidTopic>" after retry attempts of "<retryAttempts>"
    And Stubbed Company Metrics API should be called "<times>" times

    Examples:
      | companyNumber | KafkaTopic             | KafkaInvalidTopic                                       | times | retryAttempts |
      | 12345678      | stream-company-charges | stream-company-charges-company-metrics-consumer-invalid | 1     | 0             |

  Scenario Outline: Processing message and metrics api returning service unavailable error

    Given Company Metrics API returns SERVICE_UNAVAILABLE status code
    And  Charges Data API returns OK status code for relevant "<companyNumber>"
    When A message for "<companyNumber>" and changed eventType is successfully sent to the Kafka topic "<KafkaTopic>"
    Then The message should be moved to topic "<KafkaErrorTopic>" after retry attempts of "<retryAttempts>"
    And Stubbed Company Metrics API should be called "<times>" times

    Examples:
      | companyNumber | KafkaTopic             | KafkaErrorTopic                                       | retryAttempts | times |
      | 12345678      | stream-company-charges | stream-company-charges-company-metrics-consumer-error | 4             | 4     |

  Scenario Outline: Processing message and charges data api returning not found error

    Given Company Metrics API returns OK status code
    And Charges Data API returns NOT_FOUND status code for relevant "<companyNumber>"
    When A message for "<companyNumber>" and changed eventType is successfully sent to the Kafka topic "<KafkaTopic>"
    Then The message should be moved to topic "<KafkaErrorTopic>" after retry attempts of "<retryAttempts>"
    And Stubbed Company Metrics API should be called "<times>" times

    Examples:
      | companyNumber | KafkaTopic             | KafkaErrorTopic                                       | retryAttempts | times |
      | 12345678      | stream-company-charges | stream-company-charges-company-metrics-consumer-error | 4             | 0     |
