package org.interledger.spsp.server.rest;

import org.interledger.spsp.server.HermesServerApplication;

import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@SpringBootTest(
  classes = {HermesServerApplication.class, AccountsRestControllerTests.TestConfig.class},
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
  properties = {"spring.main.allow-bean-definition-overriding=true"})
public class PaymentControllerTests {

}
