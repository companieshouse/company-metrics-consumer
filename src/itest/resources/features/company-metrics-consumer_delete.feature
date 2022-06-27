Feature: Process Company Metrics Consumer delete events

  Scenario Outline: Successfully Process a valid delete message
    Given Company Metrics Consumer component is successfully running
    And Stubbed Company Metrics API endpoint will return <metricsApiStatusCode> http response code
    And  Charges Data API endpoint is stubbed for "<companyNumber>" and "<chargeId>" and will return "<chargeApiStatusCode>" http response code
    When A valid avro message "<payload>" with deleted event for "<companyNumber>" and "<resourceUriFormat>" and "<chargeId>" is sent to the Kafka topic "<KafkaTopic>"
    Then The message is successfully consumed and company number is successfully extracted to call charges-data-api GET endpoint
    Then The message is successfully consumed and company number is successfully extracted to call company-metrics-api POST endpoint with expected payload


    Examples:
      | companyNumber | chargeId                    | KafkaTopic             | resourceUriFormat      | metricsApiStatusCode | chargeApiStatusCode | payload             |
      | 01203396      | MYdKM_YnzAmJ8JtSgVXr61n1bgg | stream-company-charges | /company/%s/charges/%s | 200                  | 410                 | charges-record.json |


  Scenario Outline: A valid delete message with invalid resource uri should fail to process and message should forwarded to invalid topic
    Given Company Metrics Consumer component is successfully running
    And Stubbed Company Metrics API endpoint will return <metricsApiStatusCode> http response code
    And  Charges Data API endpoint is stubbed for "<companyNumber>" and "<chargeId>" and will return "<chargeApiStatusCode>" http response code
    When A valid avro message "<payload>" with deleted event for "<companyNumber>" and "<resourceUriFormat>" and "<chargeId>" is sent to the Kafka topic "<KafkaTopic>"
    Then The message is successfully consumed only once from the "<KafkaTopic>" topic but failed to process
    But  Failed to process and immediately moved the message into "stream-company-charges-company-metrics-consumer-invalid" topic
    And  Metrics Data API endpoint is never invoked

    Examples:
      | companyNumber | chargeId                    | KafkaTopic             | resourceUriFormat | metricsApiStatusCode | chargeApiStatusCode | payload             |
      | 01203396      | MYdKM_YnzAmJ8JtSgVXr61n1bgg | stream-company-charges | /company//charges | 200                  | 410                 | charges-record.json |


  Scenario Outline: When company metrics returns 400 then a valid delete message should fail to process and message should forwarded to invalid topic
    Given Company Metrics Consumer component is successfully running
    And Stubbed Company Metrics API endpoint will return <metricsApiStatusCode> http response code
    And  Charges Data API endpoint is stubbed for "<companyNumber>" and "<chargeId>" and will return "<chargeApiStatusCode>" http response code
    When A valid avro message "<payload>" with deleted event for "<companyNumber>" and "<resourceUriFormat>" and "<chargeId>" is sent to the Kafka topic "<KafkaTopic>"
    Then The message is successfully consumed only once from the "<KafkaTopic>" topic but failed to process
    But  Failed to process and immediately moved the message into "stream-company-charges-company-metrics-consumer-invalid" topic


    Examples:
      | companyNumber | chargeId                    | KafkaTopic             | resourceUriFormat      | metricsApiStatusCode | chargeApiStatusCode | payload                                              |
      | 01203396      | MYdKM_YnzAmJ8JtSgVXr61n1bgg | stream-company-charges | /company/%s/charges/%s | 400                  | 410                 | charges-record.json                                  |
      | 01203396      | MYdKM_YnzAmJ8JtSgVXr61n1bgg | stream-company-charges | /company/%s/charges/%s | 400                  | 410                 | Additional_notices_Happy_Path.json                   |
      | 01203396      | MYdKM_YnzAmJ8JtSgVXr61n1bgg | stream-company-charges | /company/%s/charges/%s | 400                  | 410                 | alterations_to_order_alteration_to_prohibitions.json |
      | 01203396      | MYdKM_YnzAmJ8JtSgVXr61n1bgg | stream-company-charges | /company/%s/charges/%s | 400                  | 410                 | assets_ceased_released_Happy_Path.json               |
      | 01203396      | MYdKM_YnzAmJ8JtSgVXr61n1bgg | stream-company-charges | /company/%s/charges/%s | 400                  | 410                 | obligations_secured_nature_of_charge_Happy_Path.json |
      | 01203396      | MYdKM_YnzAmJ8JtSgVXr61n1bgg | stream-company-charges | /company/%s/charges/%s | 400                  | 410                 | Scottish_Alterations.json                            |


  Scenario Outline:  When company metrics returns 503 then a valid delete message should fail to process and message should forwarded to error topic
    Given Company Metrics Consumer component is successfully running
    And Stubbed Company Metrics API endpoint will return <metricsApiStatusCode> http response code
    And  Charges Data API endpoint is stubbed for "<companyNumber>" and "<chargeId>" and will return "<chargeApiStatusCode>" http response code
    When A valid avro message "<payload>" with deleted event for "<companyNumber>" and "<resourceUriFormat>" and "<chargeId>" is sent to the Kafka topic "<KafkaTopic>"
    Then The message is successfully consumed only once from the "<KafkaTopic>" topic but failed to process
    But  Failed to process and moved the message into "stream-company-charges-company-metrics-consumer-error" topic after 4 attempts

    Examples:
      | companyNumber | chargeId                    | resourceUriFormat      | KafkaTopic             | metricsApiStatusCode | chargeApiStatusCode | payload                                              |
      | 01203396      | MYdKM_YnzAmJ8JtSgVXr61n1bgg | /company/%s/charges/%s | stream-company-charges | 503                  | 410                 | charges-record.json                                  |
      | 01203396      | MYdKM_YnzAmJ8JtSgVXr61n1bgg | /company/%s/charges/%s | stream-company-charges | 503                  | 410                 | Additional_notices_Happy_Path.json                   |
      | 01203396      | MYdKM_YnzAmJ8JtSgVXr61n1bgg | /company/%s/charges/%s | stream-company-charges | 503                  | 410                 | alterations_to_order_alteration_to_prohibitions.json |
      | 01203396      | MYdKM_YnzAmJ8JtSgVXr61n1bgg | /company/%s/charges/%s | stream-company-charges | 503                  | 410                 | assets_ceased_released_Happy_Path.json               |
      | 01203396      | MYdKM_YnzAmJ8JtSgVXr61n1bgg | /company/%s/charges/%s | stream-company-charges | 503                  | 410                 | obligations_secured_nature_of_charge_Happy_Path.json |
      | 01203396      | MYdKM_YnzAmJ8JtSgVXr61n1bgg | /company/%s/charges/%s | stream-company-charges | 503                  | 410                 | Scottish_Alterations.json                            |


  Scenario Outline:  When charges data api returns 200 then a valid delete message should fail to process and message should forwarded to error topic
    Given Company Metrics Consumer component is successfully running
    And Stubbed Company Metrics API endpoint will return <metricsApiStatusCode> http response code
    And  Charges Data API endpoint is stubbed for "<companyNumber>" and "<chargeId>" and will return "<chargeApiStatusCode>" http response code
    When A valid avro message "<payload>" with deleted event for "<companyNumber>" and "<resourceUriFormat>" and "<chargeId>" is sent to the Kafka topic "<KafkaTopic>"
    Then The message is successfully consumed only once from the "<KafkaTopic>" topic but failed to process
    But  Failed to process and moved the message into "stream-company-charges-company-metrics-consumer-error" topic after 4 attempts
    And  Metrics Data API endpoint is never invoked

    Examples:
      | companyNumber | chargeId                    | resourceUriFormat      | KafkaTopic             | metricsApiStatusCode | chargeApiStatusCode | payload                                              |
      | 01203396      | MYdKM_YnzAmJ8JtSgVXr61n1bgg | /company/%s/charges/%s | stream-company-charges | 200                  | 200                 | charges-record.json                                  |
      | 01203396      | MYdKM_YnzAmJ8JtSgVXr61n1bgg | /company/%s/charges/%s | stream-company-charges | 200                  | 200                 | Additional_notices_Happy_Path.json                   |
      | 01203396      | MYdKM_YnzAmJ8JtSgVXr61n1bgg | /company/%s/charges/%s | stream-company-charges | 200                  | 200                 | alterations_to_order_alteration_to_prohibitions.json |
      | 01203396      | MYdKM_YnzAmJ8JtSgVXr61n1bgg | /company/%s/charges/%s | stream-company-charges | 200                  | 200                 | assets_ceased_released_Happy_Path.json               |
      | 01203396      | MYdKM_YnzAmJ8JtSgVXr61n1bgg | /company/%s/charges/%s | stream-company-charges | 200                  | 200                 | obligations_secured_nature_of_charge_Happy_Path.json |
      | 01203396      | MYdKM_YnzAmJ8JtSgVXr61n1bgg | /company/%s/charges/%s | stream-company-charges | 200                  | 200                 | Scottish_Alterations.json                            |