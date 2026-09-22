package com.jvmeeye;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Security 规则:/api/** 需登录,静态资源与登录页放行。 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityRulesTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void apiRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/targets"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    void apiAccessibleAfterLogin() throws Exception {
        mockMvc.perform(get("/api/targets").with(user("admin").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targets").isArray());
    }

    @Test
    void loginPageIsPublic() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());
    }

    @Test
    void metricsWithoutTargetReturns409() throws Exception {
        mockMvc.perform(get("/api/metrics/current").with(user("admin").roles("USER")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                // RFC 7807:title 为状态描述,detail 为错误原因
                .andExpect(jsonPath("$.title").value("Conflict"))
                .andExpect(jsonPath("$.detail").exists())
                .andExpect(jsonPath("$.instance").value("/api/metrics/current"));
    }
}
