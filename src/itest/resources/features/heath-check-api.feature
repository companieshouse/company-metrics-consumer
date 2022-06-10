Feature: Health check API endpoint

  Scenario Outline: Client invokes GET /healthcheck endpoint
    Given the application running
    When the client invokes <url> endpoint
    Then the client receives status code of <code>
    And the client receives response body as <response>
    Examples:
      | url                                     | code | response          |
      | '/company-metrics-consumer/healthcheck' | 200  | '{"status":"UP"}' |