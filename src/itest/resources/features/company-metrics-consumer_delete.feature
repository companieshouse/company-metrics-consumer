Feature: Process Company Metrics Consumer delete events

  Scenario Outline: Successfully Process a valid delete message
    Given Company Metrics Consumer component is successfully running
    And Stubbed Company Metrics API endpoint will return <statusCode> http response code
    When A valid avro message "<payload>" with deleted event for "<companyNumber>" and "<resourceUriFormat>" is sent to the Kafka topic "<KafkaTopic>"
    Then The message is successfully consumed and company number is successfully extracted to call company-metrics-api recalculate POST endpoint with expected payload

    Examples:
      | companyNumber | KafkaTopic             | resourceUriFormat   | statusCode | payload             |
      | 01203396      | stream-company-charges | /company/%s/charges | 200        | charges-record.json |


  Scenario Outline: A valid delete message with invalid resource uri should fail to process and message should forwarded to invalid topic
    Given Company Metrics Consumer component is successfully running
    And Stubbed Company Metrics API endpoint will return <statusCode> http response code
    When A valid avro message "<payload>" with deleted event for "<companyNumber>" and "<resourceUriFormat>" is sent to the Kafka topic "<KafkaTopic>"
    Then The message is successfully consumed only once from the "<KafkaTopic>" topic but failed to process
    But  Failed to process and immediately moved the message into "stream-company-charges-company-metrics-consumer-invalid" topic
    And  Metrics Data API endpoint is never invoked

    Examples:
      | companyNumber | KafkaTopic             | resourceUriFormat   | statusCode | payload             |
      | 01203396      | stream-company-charges | /company//charges   | 200        | charges-record.json |


  Scenario Outline: When company metrics returns 400 then a valid delete message should fail to process and message should forwarded to invalid topic
    Given Company Metrics Consumer component is successfully running
    And Stubbed Company Metrics API endpoint will return <statusCode> http response code
    When A valid avro message "<payload>" with deleted event for "<companyNumber>" and "<resourceUriFormat>" is sent to the Kafka topic "<KafkaTopic>"
    Then The message is successfully consumed only once from the "<KafkaTopic>" topic but failed to process
    But  Failed to process and immediately moved the message into "stream-company-charges-company-metrics-consumer-invalid" topic
    And  Metrics Data API endpoint is not invoked again

    Examples:
      | companyNumber | KafkaTopic             | resourceUriFormat   | statusCode | payload                                               |
      | 01203396      | stream-company-charges | /company/%s/charges | 400        | charges-record.json                                   |
      | 01203396      | stream-company-charges | /company/%s/charges | 400        | Additional_notices_Happy_Path.json                    |
      | 01203396      | stream-company-charges | /company/%s/charges | 400        | alterations_to_order_alteration_to_prohibitions.json  |
      | 01203396      | stream-company-charges | /company/%s/charges | 400        | assets_ceased_released_Happy_Path.json                |
      | 01203396      | stream-company-charges | /company/%s/charges | 400        | obligations_secured_nature_of_charge_Happy_Path.json  |
      | 01203396      | stream-company-charges | /company/%s/charges | 400        | Scottish_Alterations.json                             |


  Scenario Outline:  When company metrics returns 503 then a valid delete message should fail to process and message should forwarded to invalid topic
    Given Company Metrics Consumer component is successfully running
    And Stubbed Company Metrics API endpoint will return <statusCode> http response code
    When A valid avro message "<payload>" with deleted event for "<companyNumber>" and "<resourceUriFormat>" is sent to the Kafka topic "<KafkaTopic>"
    Then The message is successfully consumed only once from the "<KafkaTopic>" topic but failed to process
    But  Failed to process and moved the message into "stream-company-charges-company-metrics-consumer-error" topic after 4 attempts

    Examples:
      | companyNumber | KafkaTopic             | resourceUriFormat   | statusCode | payload                                               |
      | 01203396      | stream-company-charges | /company/%s/charges | 503        | charges-record.json                                   |
      | 01203396      | stream-company-charges | /company/%s/charges | 503        | Additional_notices_Happy_Path.json                    |
      | 01203396      | stream-company-charges | /company/%s/charges | 503        | alterations_to_order_alteration_to_prohibitions.json  |
      | 01203396      | stream-company-charges | /company/%s/charges | 503        | assets_ceased_released_Happy_Path.json                |
      | 01203396      | stream-company-charges | /company/%s/charges | 503        | obligations_secured_nature_of_charge_Happy_Path.json  |
      | 01203396      | stream-company-charges | /company/%s/charges | 503        | Scottish_Alterations.json                             |