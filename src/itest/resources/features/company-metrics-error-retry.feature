Feature: Process company metrics charges-stream error and retry scenarios

  Scenario Outline: Processing invalid message

    Given Company Metrics Consumer component is running and Company Metrics API is stubbed
    When A non-avro message "<avroMessage>" is sent to the Kafka topic "<KafkaTopic>" and stubbed Company Metrics API returns "<statusCode>"
    Then The message should be moved to topic "<KafkaInvalidTopic>" after retry attempts of "<retryAttempts>"
    And Stubbed Company Metrics API should be called "<times>" times
    Examples:
      | avroMessage         | KafkaTopic             | KafkaInvalidTopic                                       | statusCode | times | retryAttempts |
      | non_avro_msg_1.avro | stream-company-charges | stream-company-charges-company-metrics-consumer-invalid | 200        | 0     | 0             |


  Scenario Outline: Processing valid avro message with invalid resource uri

    Given Company Metrics Consumer component is running and Company Metrics API is stubbed
    When A valid avro message for "<companyNumber>" and "<resourceUriFormat>" is generated and sent to the Kafka topic "<KafkaTopic>" and stubbed Company Metrics API returns "<statusCode>"
    Then The message should be moved to topic "<KafkaInvalidTopic>" after retry attempts of "<retryAttempts>"
    And Stubbed Company Metrics API should be called "<times>" times
    Examples:
      | companyNumber | resourceUriFormat      | KafkaTopic             | KafkaInvalidTopic                                       | statusCode | times | retryAttempts |
      | 12345678      | /companyabc/%s/charges | stream-company-charges | stream-company-charges-company-metrics-consumer-invalid | 200        | 0     | 0             |


  Scenario Outline: Processing valid avro message and backend api returning 400 bad request error

    Given Company Metrics Consumer component is running and Company Metrics API is stubbed
    When A valid avro message for "<companyNumber>" and "<resourceUriFormat>" is generated and sent to the Kafka topic "<KafkaTopic>" and stubbed Company Metrics API returns "<statusCode>"
    Then The message should be moved to topic "<KafkaInvalidTopic>" after retry attempts of "<retryAttempts>"
    And Stubbed Company Metrics API should be called "<times>" times
    Examples:
      | companyNumber | resourceUriFormat   | KafkaTopic             | KafkaInvalidTopic                                       | statusCode | times | retryAttempts |
      | 12345678      | /company/%s/charges | stream-company-charges | stream-company-charges-company-metrics-consumer-invalid | 400        | 1     | 0             |


  Scenario Outline: Processing valid avro message and backend api returning 503 bad request error

    Given Company Metrics Consumer component is running and Company Metrics API is stubbed
    When A valid avro message for "<companyNumber>" and "<resourceUriFormat>" is generated and sent to the Kafka topic "<KafkaTopic>" and stubbed Company Metrics API returns "<statusCode>"
    Then The message should be moved to topic "<KafkaInvalidTopic>" after retry attempts of "<retryAttempts>"
    And Stubbed Company Metrics API should be called "<times>" times
    Examples:
      | companyNumber | resourceUriFormat   | KafkaTopic             | KafkaInvalidTopic                                     | statusCode | retryAttempts | times |
      | 12345678      | /company/%s/charges | stream-company-charges | stream-company-charges-company-metrics-consumer-error | 503        | 4             | 4     |
