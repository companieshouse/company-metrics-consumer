Feature: Process company metrics charges-stream happy path processing

  Scenario Outline: Consume a generated message, process it then call charges data api
    Given Company Metrics Consumer component is running and Company Metrics API is stubbed
    When A valid avro message for "<companyNumber>" and "<resourceUriFormat>" is generated and sent to the Kafka topic "<KafkaTopic>" and stubbed Company Metrics API returns "<statusCode>"
    Then The message is successfully consumed and company number is susccessfully extracted to call company-metrics-api POST endpoint with expected payload

    Examples:
      | companyNumber | KafkaTopic             | resourceUriFormat   | statusCode |
      | 01203396      | stream-company-charges | /company/%s/charges | 200        |
      | 08124207      | stream-company-charges | /company/%s/charges | 200        |
      | SC109614      | stream-company-charges | /company/%s/charges | 200        |
