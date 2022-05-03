Feature: Process Company Metrics Consumer information

  Scenario Outline: Consume a generated message, process it then call charges data api
    Given Company Metrics Consumer component is running and Company Metrics API is stubbed
    When A valid avro message for "<companyNumber>" and "<resourceUriFormat>" is generated and sent to the Kafka topic "<KafkaTopic>" and stubbed Company Metrics API returns "<statusCode>"
    Then The message is successfully consumed and company number is susccessfully extracted to call company-metrics-api POST endpoint with expected payload

    Examples:
      | companyNumber | KafkaTopic             | resourceUriFormat   | statusCode |
      | 01203396      | stream-company-charges | /company/%s/charges | 200        |
      | 08124207      | stream-company-charges | /company/%s/charges | 200        |
      | SC109614      | stream-company-charges | /company/%s/charges | 200        |

  Scenario Outline: Processing invalid message

    Given Company Metrics Consumer component is running and Company Metrics API is stubbed
    When A non-avro message "<avroMessage>" is sent to the Kafka topic "<KafkaTopic>" and stubbed Company Metrics API returns "<statusCode>"
    Then The message should be moved to topic "<KafkaInvalidTopic>"
    Examples:
      | avroMessage         | KafkaTopic             | KafkaInvalidTopic                                       | statusCode |
      | non_avro_msg_1.avro | stream-company-charges | stream-company-charges-company-metrics-consumer-invalid | 200        |


  Scenario Outline: Processing valid avro message with invalid resource uri

    Given Company Metrics Consumer component is running and Company Metrics API is stubbed
    When A valid avro message for "<companyNumber>" and "<resourceUriFormat>" is generated and sent to the Kafka topic "<KafkaTopic>" and stubbed Company Metrics API returns "<statusCode>"
    Then The message should be moved to topic "<KafkaInvalidTopic>"
    Examples:
      | companyNumber | resourceUriFormat      | KafkaTopic             | KafkaInvalidTopic                                       | statusCode |
      | 12345678      | /companyabc/%s/charges | stream-company-charges | stream-company-charges-company-metrics-consumer-invalid | 200        |


  Scenario Outline: Processing valid avro message and backend api returning 400 bad request error

    Given Company Metrics Consumer component is running and Company Metrics API is stubbed
    When A valid avro message for "<companyNumber>" and "<resourceUriFormat>" is generated and sent to the Kafka topic "<KafkaTopic>" and stubbed Company Metrics API returns "<statusCode>"
    Then The message should be moved to topic "<KafkaInvalidTopic>"
    Examples:
      | companyNumber | resourceUriFormat   | KafkaTopic             | KafkaInvalidTopic                                       | statusCode |
      | 12345678      | /company/%s/charges | stream-company-charges | stream-company-charges-company-metrics-consumer-invalid | 400        |


  Scenario Outline: Processing valid avro message and backend api returning 503 bad request error

    Given Company Metrics Consumer component is running and Company Metrics API is stubbed
    When A valid avro message for "<companyNumber>" and "<resourceUriFormat>" is generated and sent to the Kafka topic "<KafkaTopic>" and stubbed Company Metrics API returns "<statusCode>"
    Then The message should be moved to topic "<KafkaInvalidTopic>" after retry attempts of "<retryAttempts>"
    Examples:
      | companyNumber | resourceUriFormat   | KafkaTopic             | KafkaInvalidTopic                                     | statusCode | retryAttempts |
      | 12345678      | /company/%s/charges | stream-company-charges | stream-company-charges-company-metrics-consumer-error | 503        | 3             |
