Feature: Process company metrics charges-stream happy path processing

  Scenario Outline: Consume a generated message, process it then call charges data api
    Given Company Metrics Consumer component is running and Company Metrics API is stubbed
    And  Charges Data API endpoint is stubbed for "<companyNumber>" and "<chargeId>" and will return "<chargeApiStatusCode>" http response code
    When A valid avro message for "<companyNumber>" and "<resourceUriFormat>" and "<chargeId>" is generated and sent to the Kafka topic "<KafkaTopic>" and stubbed Company Metrics API returns "<metricsApiStatusCode>"
    Then The message is successfully consumed and company number is successfully extracted to call charges-data-api GET endpoint
    Then The message is successfully consumed and company number is successfully extracted to call company-metrics-api POST endpoint with expected payload

    Examples:
      | companyNumber | KafkaTopic             | resourceUriFormat      | metricsApiStatusCode | chargeApiStatusCode | chargeId                    |
      | 01203396      | stream-company-charges | /company/%s/charges/%s | 200                  | 200                 | MYdKM_YnzAmJ8JtSgVXr61n1bgg |
      | 08124207      | stream-company-charges | /company/%s/charges/%s | 200                  | 200                 | MYdKM_YnzAmJ8JtSgVXr61n1bgg |
      | SC109614      | stream-company-charges | /company/%s/charges/%s | 200                  | 200                 | MYdKM_YnzAmJ8JtSgVXr61n1bgg |

