package io.pivotal.security.controller.v1.permissions;

import com.greghaskins.spectrum.Spectrum;
import io.pivotal.security.CredentialManagerApp;
import io.pivotal.security.request.AccessControlEntry;
import io.pivotal.security.request.AccessEntryRequest;
import io.pivotal.security.service.AccessControlService;
import io.pivotal.security.util.DatabaseProfileResolver;
import io.pivotal.security.view.AccessEntryResponse;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Collections;

import static com.greghaskins.spectrum.Spectrum.*;
import static io.pivotal.security.helper.SpectrumHelper.wireAndUnwire;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(Spectrum.class)
@SpringBootTest(classes = CredentialManagerApp.class)
@ActiveProfiles(value = "unit-test", resolver = DatabaseProfileResolver.class)
public class AccessEntryControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockBean
    private AccessControlService accessControlService;

    private MockMvc mockMvc;

    {
        wireAndUnwire(this);

        beforeEach(() -> {
            mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        });

        describe("When posting access control entry for user and credential", () -> {
            it("returns the full Access Control List for user", () -> {
                final MockHttpServletRequestBuilder post = post("/api/v1/resources/aces")
                        .accept(APPLICATION_JSON)
                        .contentType(APPLICATION_JSON)
                        .content("{" +
                                "  \"resource\": \"cred1\",\n" +
                                "  \"aces\": [\n" +
                                "     { \n" +
                                "       \"actor\": \"dan\",\n" +
                                "       \"operations\": [\"read\"]\n" +
                                "     }]" +
                                "}");

                AccessControlEntry entry = new AccessControlEntry("dan", Collections.singletonList("read"));
                AccessEntryResponse response = new AccessEntryResponse("cred1", Collections.singletonList(entry));

                when(accessControlService.setAccessControlEntry(any(AccessEntryRequest.class))).thenReturn(response);

                this.mockMvc.perform(post).andExpect(status().isOk())
                        .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                        .andExpect(jsonPath("$.resource", equalTo("cred1")))
                        .andExpect(jsonPath("$.acls", hasSize(1)))
                        .andExpect(jsonPath("$.acls[0].actor", equalTo("dan")))
                        .andExpect(jsonPath("$.acls[0].operations[0]", equalTo("read")));

                ArgumentCaptor<AccessEntryRequest> captor = ArgumentCaptor.forClass(AccessEntryRequest.class);
                verify(accessControlService).setAccessControlEntry(captor.capture());

                assertThat(captor.getValue().getResource(), equalTo("cred1"));
                assertThat(captor.getValue().getAces().get(0).getActor(), equalTo("dan"));
                assertThat(captor.getValue().getAces().get(0).getOperations().get(0), equalTo("read"));
            });
        });
    }
}
