package com.gertidokaj.geocento.java_full_stack_assessment;

import com.gertidokaj.geocento.java_full_stack_assessment.config.TestSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestSecurityConfig.class)
class JavaFullStackAssessmentApplicationTests {

    @Test
    void contextLoads() {
    }

}
