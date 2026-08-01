package com.securebank.accounts;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "securebank.accounts.otp.expose-code=true")
@AutoConfigureMockMvc
class AccountsControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void primaryAccountIncludesFreezeState() throws Exception {
    mockMvc
      .perform(get("/api/v1/accounts/primary"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.id").value("acc-demo-primary"))
      .andExpect(jsonPath("$.frozen").value(false))
      .andExpect(jsonPath("$.status").value("Active - Verified"));
  }

  @Test
  void statementEndpointReturnsPdfDownload() throws Exception {
    byte[] pdf = mockMvc
      .perform(get("/api/v1/accounts/acc-demo-primary/statement"))
      .andExpect(status().isOk())
      .andExpect(
        header().string("Content-Disposition", "attachment; filename=\"securebank-statement.pdf\"")
      )
      .andExpect(content().contentType(MediaType.APPLICATION_PDF))
      .andReturn()
      .getResponse()
      .getContentAsByteArray();

    try (PDDocument document = Loader.loadPDF(pdf)) {
      String text = new PDFTextStripper().getText(document);
      Assertions.assertThat(text)
        .contains("Bank Statement")
        .contains("Account Summary")
        .contains("Transactions")
        .contains("Kaveesha Kapitiarachchi");
    }
  }

  @Test
  void transactionHistorySupportsFiltering() throws Exception {
    mockMvc
      .perform(
        get("/api/v1/accounts/acc-demo-primary/transactions")
          .param("direction", "OUT")
          .param("dateFrom", "2026-07-20")
          .param("dateTo", "2026-07-20")
          .param("minAmount", "40")
          .param("maxAmount", "60")
          .param("type", "CARD_PAYMENT")
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.length()").value(1))
      .andExpect(jsonPath("$[0].merchant").value("Kumar's Grocers"))
      .andExpect(jsonPath("$[0].transactionType").value("CARD_PAYMENT"));
  }

  @Test
  void freezeFlowBelongsToAccountsService() throws Exception {
    String response = mockMvc
      .perform(
        post("/api/v1/accounts/acc-demo-primary/freeze")
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"reason\":\"Card lost\"}")
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.type").value("FREEZE_ACCOUNT"))
      .andReturn()
      .getResponse()
      .getContentAsString();

    String changeRequestId = com.jayway.jsonpath.JsonPath.read(response, "$.changeRequestId");

    mockMvc
      .perform(
        post("/api/v1/accounts/changes/" + changeRequestId + "/confirm")
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"otpCode\":\"000000\"}")
      )
      .andExpect(status().isBadRequest());
  }

  @Test
  void accountLinkRequiresMatchingOwnershipAndOtp() throws Exception {
    mockMvc
      .perform(
        post("/api/v1/accounts/link")
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"accountNumber\":\"1234567890\",\"nationalIdOrPassport\":\"wrong-id\"}")
      )
      .andExpect(status().isNotFound())
      .andExpect(
        jsonPath("$.message").value("We could not match those details to an account owned by you")
      );

    String response = mockMvc
      .perform(
        post("/api/v1/accounts/link")
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"accountNumber\":\"1234567890\",\"nationalIdOrPassport\":\"200229602936\"}")
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.type").value("LINK_ACCOUNT"))
      .andExpect(jsonPath("$.deliveryTarget").value(""))
      .andReturn()
      .getResponse()
      .getContentAsString();

    String changeRequestId = com.jayway.jsonpath.JsonPath.read(response, "$.changeRequestId");
    String otpCode = com.jayway.jsonpath.JsonPath.read(response, "$.demoCode");

    mockMvc
      .perform(
        post("/api/v1/accounts/link/" + changeRequestId + "/confirm")
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"otpCode\":\"" + otpCode + "\"}")
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.account.id").value("acc-demo-savings"))
      .andExpect(jsonPath("$.account.accountNumber").value("1234567890"));

    mockMvc
      .perform(get("/api/v1/accounts"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.length()").value(2));
  }

  @Test
  void openingAccountCreatesOneAssociatedDebitCard() throws Exception {
    String response = mockMvc
      .perform(
        post("/api/v1/accounts/open")
          .contentType(MediaType.APPLICATION_JSON)
          .content(
            "{\"accountType\":\"SAVINGS\",\"productCode\":\"SAV-GOAL\",\"ownershipType\":\"INDIVIDUAL\"}"
          )
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.type").value("OPEN_ACCOUNT"))
      .andReturn()
      .getResponse()
      .getContentAsString();

    String changeRequestId = com.jayway.jsonpath.JsonPath.read(response, "$.changeRequestId");
    String otpCode = com.jayway.jsonpath.JsonPath.read(response, "$.demoCode");
    String confirmation = mockMvc
      .perform(
        post("/api/v1/accounts/open/" + changeRequestId + "/confirm")
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"otpCode\":\"" + otpCode + "\"}")
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.account.nickname").value("Goal Savings"))
      .andReturn()
      .getResponse()
      .getContentAsString();

    String accountId = com.jayway.jsonpath.JsonPath.read(confirmation, "$.account.id");
    mockMvc
      .perform(get("/api/v1/accounts/" + accountId))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.ownershipLabel").value("Individual account"))
      .andExpect(jsonPath("$.cards.length()").value(1))
      .andExpect(jsonPath("$.cards[0].cardType").value("DEBIT"));
  }

  @Test
  void creditCardIsLinkedOnlyToChosenAccount() throws Exception {
    String response = mockMvc
      .perform(
        post("/api/v1/accounts/cards/link")
          .contentType(MediaType.APPLICATION_JSON)
          .content(
            "{\"cardNumber\":\"4485 1234 1234 5678\",\"expiryDate\":\"11/29\",\"nationalIdOrPassport\":\"200229602936\",\"accountId\":\"acc-demo-primary\"}"
          )
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.type").value("LINK_CREDIT_CARD"))
      .andReturn()
      .getResponse()
      .getContentAsString();

    String changeRequestId = com.jayway.jsonpath.JsonPath.read(response, "$.changeRequestId");
    String otpCode = com.jayway.jsonpath.JsonPath.read(response, "$.demoCode");
    mockMvc
      .perform(
        post("/api/v1/accounts/cards/link/" + changeRequestId + "/confirm")
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"otpCode\":\"" + otpCode + "\"}")
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.card.accountId").value("acc-demo-primary"))
      .andExpect(jsonPath("$.card.cardType").value("CREDIT"));

    mockMvc
      .perform(get("/api/v1/accounts/acc-demo-primary"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.cards.length()").value(2));
  }
}
