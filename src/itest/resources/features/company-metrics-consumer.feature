Feature: Process Company Metrics Delta information

  Scenario Outline: Consume a generated message, process it then call charges data api
    Given Company Metrics Consumer component is running and Charges Data API is stubbed
    When A valid avro message for "<companyNumber>" is generated and sent to the Kafka topic "<KafkaTopic>"
    Then The message is successfully consumed and company number is susccessfully extracted to call company-metrics-api POST endpoint with expected payload

    Examples:
      | companyNumber | KafkaTopic             |
      | 01203396      | stream-company-charges |
      | 08124207      | stream-company-charges |
      | SC109614      | stream-company-charges |