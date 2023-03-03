Feature: Process Company Metrics Consumer delete events

  Scenario Outline: Successfully Process a valid delete message
    Given Company Metrics API returns OK status code
    And  Charges Data API returns NOT_FOUND status code for relevant "<companyNumber>"
    When A message with "<payload>" and deleted eventType for "<companyNumber>" is sent to the Kafka topic "<KafkaTopic>"
    Then The message is successfully consumed and calls charges-data-api
    Then The message is successfully consumed and calls company-metrics-api with expected payload


    Examples:
      | companyNumber | KafkaTopic             | payload             |
      | 01203396      | stream-company-charges | charges-record.json |


  Scenario Outline: A valid delete message with invalid resource uri should fail to process and be forwarded to the invalid topic
    Given Company Metrics API returns OK status code
    And  Charges Data API returns NOT_FOUND status code for relevant "<companyNumber>"
    When A message with invalid resourceURI and "<payload>" for "<companyNumber>" is sent to the Kafka topic "<KafkaTopic>"
    Then The message is successfully consumed only once from the "<KafkaTopic>" topic but failed to process
    But  Failed to process and immediately moved the message into "stream-company-charges-company-metrics-consumer-invalid" topic
    And  Metrics Data API endpoint is never invoked

    Examples:
      | companyNumber | KafkaTopic             | payload             |
      | 01203396      | stream-company-charges | charges-record.json |


  Scenario Outline: When company metrics returns Bad Request then a valid delete message should fail to process and message should forwarded to invalid topic
    Given Company Metrics API returns BAD_REQUEST status code
    And  Charges Data API returns NOT_FOUND status code for relevant "<companyNumber>"
    When A message with "<payload>" and deleted eventType for "<companyNumber>" is sent to the Kafka topic "<KafkaTopic>"
    Then The message is successfully consumed only once from the "<KafkaTopic>" topic but failed to process
    But  Failed to process and immediately moved the message into "stream-company-charges-company-metrics-consumer-invalid" topic


    Examples:
      | companyNumber | KafkaTopic             | payload                                              |
      | 01203396      | stream-company-charges | charges-record.json                                  |
      | 01203396      | stream-company-charges | Additional_notices_Happy_Path.json                   |
      | 01203396      | stream-company-charges | alterations_to_order_alteration_to_prohibitions.json |
      | 01203396      | stream-company-charges | assets_ceased_released_Happy_Path.json               |
      | 01203396      | stream-company-charges | obligations_secured_nature_of_charge_Happy_Path.json |
      | 01203396      | stream-company-charges | Scottish_Alterations.json                            |


  Scenario Outline:  When company metrics service is unavailable then a valid delete message should fail to process and message should forwarded to error topic
    Given Company Metrics API returns SERVICE_UNAVAILABLE status code
    And  Charges Data API returns NOT_FOUND status code for relevant "<companyNumber>"
    When A message with "<payload>" and deleted eventType for "<companyNumber>" is sent to the Kafka topic "<KafkaTopic>"
    Then The message is successfully consumed only once from the "<KafkaTopic>" topic but failed to process
    But  Failed to process and moved the message into "stream-company-charges-company-metrics-consumer-error" topic after 4 attempts

    Examples:
      | companyNumber | KafkaTopic             | payload                                              |
      | 01203396      | stream-company-charges | charges-record.json                                  |
      | 01203396      | stream-company-charges | Additional_notices_Happy_Path.json                   |
      | 01203396      | stream-company-charges | alterations_to_order_alteration_to_prohibitions.json |
      | 01203396      | stream-company-charges | assets_ceased_released_Happy_Path.json               |
      | 01203396      | stream-company-charges | obligations_secured_nature_of_charge_Happy_Path.json |
      | 01203396      | stream-company-charges | Scottish_Alterations.json                            |


  Scenario Outline:  When charges data api returns OK response then a valid delete message should fail to process and message should forwarded to error topic
    Given Company Metrics API returns OK status code
    And  Charges Data API returns OK status code for relevant "<companyNumber>"
    When A message with "<payload>" and deleted eventType for "<companyNumber>" is sent to the Kafka topic "<KafkaTopic>"
    Then The message is successfully consumed only once from the "<KafkaTopic>" topic but failed to process
    But  Failed to process and moved the message into "stream-company-charges-company-metrics-consumer-error" topic after 4 attempts
    And  Metrics Data API endpoint is never invoked

    Examples:
      | companyNumber | KafkaTopic             | payload                                              |
      | 01203396      | stream-company-charges | charges-record.json                                  |
      | 01203396      | stream-company-charges | Additional_notices_Happy_Path.json                   |
      | 01203396      | stream-company-charges | alterations_to_order_alteration_to_prohibitions.json |
      | 01203396      | stream-company-charges | assets_ceased_released_Happy_Path.json               |
      | 01203396      | stream-company-charges | obligations_secured_nature_of_charge_Happy_Path.json |
      | 01203396      | stream-company-charges | Scottish_Alterations.json                            |