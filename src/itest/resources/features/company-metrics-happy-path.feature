Feature: Process company metrics charges-stream happy path processing

  Scenario Outline: Consume a message, process it correctly then call charges data and company metrics api
    Given Charges Data API returns OK status code for relevant "<companyNumber>"
    And Company Metrics API returns OK status code
    When A message for "<companyNumber>" is successfully sent to the Kafka topic "<KafkaTopic>"
    Then The message is successfully consumed and calls charges-data-api
    Then The message is successfully consumed and calls company-metrics-api with expected payload

    Examples:
      | companyNumber | KafkaTopic             |
      | 01203396      | stream-company-charges |
      | 08124207      | stream-company-charges |
      | SC109614      | stream-company-charges |
