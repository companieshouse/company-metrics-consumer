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
    And Charges Data API endpoint is stubbed for "<companyNumber>" and "<chargeId>" and will return "<chargeApiStatusCode>" http response code
    When A valid avro message for "<companyNumber>" and "<resourceUriFormat>" and "<chargeId>" is generated and sent to the Kafka topic "<KafkaTopic>" and stubbed Company Metrics API returns "<metricsApiStatusCode>"
    Then The message should be moved to topic "<KafkaInvalidTopic>" after retry attempts of "<retryAttempts>"
    And Stubbed Company Metrics API should be called "<times>" times
    Examples:
      | companyNumber | chargeId                    | resourceUriFormat      | KafkaTopic             | KafkaInvalidTopic                                       | metricsApiStatusCode | chargeApiStatusCode | times | retryAttempts |
      | 12345678      | MYdKM_YnzAmJ8JtSgVXr61n1bgg | /companyabc/%s/charges | stream-company-charges | stream-company-charges-company-metrics-consumer-invalid | 200                  | 200                 | 0     | 0             |


  Scenario Outline: Processing valid avro message and backend api returning 400 bad request error

    Given Company Metrics Consumer component is running and Company Metrics API is stubbed
    And Charges Data API endpoint is stubbed for "<companyNumber>" and "<chargeId>" and will return "<chargeApiStatusCode>" http response code
    When A valid avro message for "<companyNumber>" and "<resourceUriFormat>" and "<chargeId>" is generated and sent to the Kafka topic "<KafkaTopic>" and stubbed Company Metrics API returns "<metricsApiStatusCode>"
    Then The message should be moved to topic "<KafkaInvalidTopic>" after retry attempts of "<retryAttempts>"
    And Stubbed Company Metrics API should be called "<times>" times
    Examples:
      | companyNumber | chargeId                    | resourceUriFormat      | KafkaTopic             | KafkaInvalidTopic                                       | metricsApiStatusCode | chargeApiStatusCode | times | retryAttempts |
      | 12345678      | MYdKM_YnzAmJ8JtSgVXr61n1bgg | /company/%s/charges/%s | stream-company-charges | stream-company-charges-company-metrics-consumer-invalid | 400                  | 200                 | 1     | 0             |


  Scenario Outline: Processing valid avro message and backend api returning 503 bad request error

    Given Company Metrics Consumer component is running and Company Metrics API is stubbed
    And Charges Data API endpoint is stubbed for "<companyNumber>" and "<chargeId>" and will return "<chargeApiStatusCode>" http response code
    When A valid avro message for "<companyNumber>" and "<resourceUriFormat>" and "<chargeId>" is generated and sent to the Kafka topic "<KafkaTopic>" and stubbed Company Metrics API returns "<metricsApiStatusCode>"
    Then The message should be moved to topic "<KafkaInvalidTopic>" after retry attempts of "<retryAttempts>"
    And Stubbed Company Metrics API should be called "<times>" times
    Examples:

      | companyNumber | chargeId                    | resourceUriFormat      | KafkaTopic             | KafkaInvalidTopic                                     | metricsApiStatusCode | chargeApiStatusCode | retryAttempts | times |
      | 12345678      | MYdKM_YnzAmJ8JtSgVXr61n1bgg | /company/%s/charges/%s | stream-company-charges | stream-company-charges-company-metrics-consumer-error | 503                  | 200                 | 4             | 4     |

  Scenario Outline: Processing valid avro message and charges data api returning 404 error

    Given Company Metrics Consumer component is running and Company Metrics API is stubbed
    And Charges Data API endpoint is stubbed for "<companyNumber>" and "<chargeId>" and will return "<chargeApiStatusCode>" http response code
    When A valid avro message for "<companyNumber>" and "<resourceUriFormat>" and "<chargeId>" is generated and sent to the Kafka topic "<KafkaTopic>" and stubbed Company Metrics API returns "<metricsApiStatusCode>"
    Then The message should be moved to topic "<KafkaInvalidTopic>" after retry attempts of "<retryAttempts>"
    And Stubbed Company Metrics API should be called "<times>" times
    Examples:
      | companyNumber | chargeId                    | resourceUriFormat      | KafkaTopic             | KafkaInvalidTopic                                     | metricsApiStatusCode | chargeApiStatusCode | retryAttempts | times |
      | 12345678      | MYdKM_YnzAmJ8JtSgVXr61n1bgg | /company/%s/charges/%s | stream-company-charges | stream-company-charges-company-metrics-consumer-error | 200                  | 404                 | 4             | 0     |
