Feature: Process Company Metrics Delta information

  Scenario Outline: Consume the message, process it then call charges data api

    Given Company Metrics Consumer component is running and Charges Data API is stubbed
    When A valid avro message "<avroMessage>" is sent to the Kafka topic "<KafkaTopic>"
    Then The message is successfully consumed and company number is susccessfully extracted to call company-metrics-api POST endpoint with expected payload

    Examples:
      | avroMessage     | KafkaTopic     |
      | 01203396.avro   | stream-company-charges |
      | 08124207.avro   | stream-company-charges |
      | SC109614.avro   | stream-company-charges |

