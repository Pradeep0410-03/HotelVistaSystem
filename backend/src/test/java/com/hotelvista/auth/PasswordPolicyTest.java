package com.hotelvista.auth;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class PasswordPolicyTest {
 @Test void requiresLengthUpperLowerDigitAndSymbol() {
  try(var factory=Validation.buildDefaultValidatorFactory()) {
   var validator=factory.getValidator();
   for(String password:new String[]{"Short1!","lowercaseonly1!","UPPERCASEONLY1!","NoDigitsHere!!","NoSymbolsHere123","Password123   ","A1!"+"x".repeat(126)})
    assertThat(validator.validate(new RegisterRequest("Guest","guest@example.test",password))).as(password).isNotEmpty();
   assertThat(validator.validate(new RegisterRequest("Guest","guest@example.test","Long Passphrase 7!"))).isEmpty();
  }
 }
}
