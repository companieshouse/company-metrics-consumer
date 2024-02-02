Feature: Company metrics PSC statements statements criteria

  Scenario Outline: Post psc statements recalculate successfully - event type changed
    Given The event type is "<eventType>"
    And A resource change data message for "<companyNumber>" with an psc entity exists on the "<mainKafkaTopic>" kafka topic
    And Company Metrics API returns OK status code
    When The message is consumed
    Then A request is sent to the Company Metrics Recalculate endpoint for PSCs

    Examples:
      | mainKafkaTopic        | eventType  | companyNumber |
      | stream-psc-statements | changed    | 01203396      |
      | stream-psc-statements | changed    | 08124207      |
      | stream-psc-statements | changed    | SC109614      |