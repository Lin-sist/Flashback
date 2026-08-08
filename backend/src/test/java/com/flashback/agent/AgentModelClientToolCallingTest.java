package com.flashback.agent;

import com.flashback.agent.resilience.AgentCallBudget;
import com.flashback.agent.resilience.AgentProviderException;
import com.flashback.agent.resilience.AgentProviderFailureCategory;
import com.flashback.config.AppAgentProperties;
import com.flashback.config.AppAiProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * function calling 可用性判定测试（C2）。
 *
 * 核心目的：守住 proposal F29 的结论——不得假设任意 provider / model 都支持 FC。
 * 以及 design 决策 1 的「无降级」：不可用时只是不下发 tools，不存在替代协议。
 */
class AgentModelClientToolCallingTest {

    private AppAiProperties aiProperties;
    private AppAgentProperties agentProperties;

    @BeforeEach
    void setUp() {
        aiProperties = new AppAiProperties();
        aiProperties.setProvider("deepseek");
        aiProperties.setBaseUrl("https://api.deepseek.com");
        aiProperties.setApiKey("test-key");
        aiProperties.setModel("deepseek-v4-pro");

        agentProperties = new AppAgentProperties();
        agentProperties.setToolCallingEnabled(true);
        agentProperties.setFunctionCallingModels(List.of("deepseek-v4-pro", "deepseek-v4-flash"));
    }

    @Test
    void shouldAllowToolCallingForConfirmedModel() {
        assertThat(client().toolCallingUnavailableReason()).isNull();
    }

    /**
     * F29：未确认支持 FC 的 model 一律不下发 tools。
     */
    @Test
    void shouldRejectToolCallingForUnconfirmedModel() {
        aiProperties.setModel("deepseek-reasoner");

        assertThat(client().toolCallingUnavailableReason()).isEqualTo("当前模型未确认支持function calling");
    }

    @Test
    void shouldRejectToolCallingWhenModelAllowlistEmpty() {
        agentProperties.setFunctionCallingModels(List.of());

        assertThat(client().toolCallingUnavailableReason()).isEqualTo("当前模型未确认支持function calling");
    }

    @Test
    void shouldRejectToolCallingWhenDisabledByConfig() {
        agentProperties.setToolCallingEnabled(false);

        assertThat(client().toolCallingUnavailableReason()).isEqualTo("Agent工具调用未启用");
    }

    /**
     * strict mode 需独立地址；缺失时视为配置错误而非静默降级。
     */
    @Test
    void shouldTreatStrictModeWithoutBaseUrlAsConfigurationError() {
        agentProperties.setStrictModeEnabled(true);
        agentProperties.setStrictModeBaseUrl("");

        AgentModelClient client = client();
        assertThat(client.toolCallingUnavailableReason()).isEqualTo("strict mode缺少base url配置");
        assertThat(client.useStrictMode()).isFalse();
    }

    @Test
    void shouldEnableStrictModeWhenBaseUrlConfigured() {
        agentProperties.setStrictModeEnabled(true);
        agentProperties.setStrictModeBaseUrl("https://api.deepseek.com/beta");

        AgentModelClient client = client();
        assertThat(client.toolCallingUnavailableReason()).isNull();
        assertThat(client.useStrictMode()).isTrue();
    }

    @Test
    void shouldNotUseStrictModeWhenDisabled() {
        agentProperties.setStrictModeEnabled(false);
        agentProperties.setStrictModeBaseUrl("https://api.deepseek.com/beta");

        assertThat(client().useStrictMode()).isFalse();
    }

    @Test
    void shouldInheritBaseUnavailableReasonWhenAiNotConfigured() {
        aiProperties.setApiKey("");

        assertThat(client().toolCallingUnavailableReason()).isEqualTo("AI服务未配置");
    }

    /**
     * mock provider 下允许工具，用于零外调端到端测试；来源仍标记为 mock。
     */
    @Test
    void shouldAllowToolCallingUnderMockProvider() {
        aiProperties.setProvider("mock");
        aiProperties.setRealModeMockEnabled(true);

        assertThat(client().toolCallingUnavailableReason()).isNull();
    }

    @Test
    void shouldMatchModelAllowlistCaseInsensitively() {
        aiProperties.setModel("DeepSeek-V4-Pro");

        assertThat(client().isFunctionCallingModel("DeepSeek-V4-Pro")).isTrue();
        assertThat(client().toolCallingUnavailableReason()).isNull();
    }

    @Test
    void shouldRejectBlankModel() {
        assertThat(client().isFunctionCallingModel("  ")).isFalse();
        assertThat(client().isFunctionCallingModel(null)).isFalse();
    }

    @Test
    void shouldClampHttpTimeoutToRemainingRequestBudget() throws Exception {
        aiProperties.setTimeoutMillis(20_000);
        AtomicLong now = new AtomicLong();
        AgentCallBudget budget = AgentCallBudget.start(24_000, now::get);
        now.set(10_000_000_000L);

        assertThat(client().requestTimeout(budget)).hasMillis(14_000);
    }

    @Test
    void shouldClassifyMalformedMaterialAsInvalidResponseWithoutLeakingBody() {
        assertThatThrownBy(() -> client().extractText("sensitive malformed payload", "material"))
                .isInstanceOfSatisfying(AgentProviderException.class, ex -> {
                    assertThat(ex.category()).isEqualTo(AgentProviderFailureCategory.INVALID_RESPONSE);
                    assertThat(ex.getMessage()).doesNotContain("sensitive");
                });
    }

    @Test
    void shouldRejectMissingSharedBudgetInsteadOfCreatingANestedBudget() {
        assertThatThrownBy(() -> client().requestTimeout(null))
                .isInstanceOfSatisfying(AgentProviderException.class,
                        ex -> assertThat(ex.category())
                                .isEqualTo(AgentProviderFailureCategory.AUTH_CONFIGURATION));
    }

    private AgentModelClient client() {
        return new AgentModelClient(aiProperties, agentProperties);
    }
}
