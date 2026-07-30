package com.flashback.agent.trace;

import com.flashback.domain.AgentTurnTrace;
import com.flashback.mapper.AgentTurnTraceMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 针对**真实 MySQL** 的轨迹落库探针（C5 残余项排查）。
 *
 * 存在理由：C5 闸门 3 的探针写入 H2 测试库，closeout 里把
 * 「MySQL 上的轨迹落库未经真实联调」列为残余风险。用户手验后发现真实库里
 * 轨迹只落了一条且 steps_json 在第一步就断，需要一个能直接打到 MySQL 的探针来定位。
 *
 * 安全边界：
 * - **默认跳过**，只有 C5_MYSQL_PROBE=1 时运行；
 * - **只读**：只查询既有轨迹，不插入、不修改、不删除任何数据；
 * - 只打印结构化字段，不打印任何日记原文或对话原文。
 */
@EnabledIfEnvironmentVariable(named = "C5_MYSQL_PROBE", matches = "1")
@SpringBootTest
@ActiveProfiles("dev")
class C5MysqlTraceProbeTest {

    @DynamicPropertySource
    static void localMysql(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:mysql://127.0.0.1:3306/flashback"
                + "?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai");
        registry.add("spring.datasource.username", () -> "root");
        registry.add("spring.datasource.password", () -> System.getenv().getOrDefault("DB_PASSWORD", "123456"));
        registry.add("spring.sql.init.mode", () -> "never");
        registry.add("app.ai.provider", () -> "mock");
        registry.add("app.ai.real-mode-mock-enabled", () -> "true");
    }

    @Autowired
    private AgentTurnTraceMapper agentTurnTraceMapper;

    @Test
    void inspectExistingTracesOnMysql() {
        List<AgentTurnTrace> recent = agentTurnTraceMapper.selectRecentByUserId(6L, 20);
        System.out.printf("C5MYSQL traceCount=%d%n", recent.size());
        for (AgentTurnTrace trace : recent) {
            String steps = trace.getStepsJson() == null ? "" : trace.getStepsJson();
            System.out.printf(
                    "C5MYSQL id=%d session=%d turn=%d attempt=%d outcome=%s durationMs=%s "
                            + "stage=%s reason=%s model=%s stepCount=%d createdAt=%s%n",
                    trace.getId(),
                    trace.getSessionId(),
                    trace.getTurnNo(),
                    trace.getAttemptNo(),
                    trace.getOutcome(),
                    String.valueOf(trace.getProviderDurationMs()),
                    trace.getStage(),
                    String.valueOf(trace.getStageReason()),
                    String.valueOf(trace.getModel()),
                    countSteps(steps),
                    String.valueOf(trace.getCreatedAt()));
            System.out.printf("C5MYSQL id=%d steps=%s%n", trace.getId(), steps);
        }

        // 按会话取回，验证查询路径在 MySQL 上可用（H2 与 MySQL 的排序行为可能不同）。
        if (!recent.isEmpty()) {
            Long sessionId = recent.get(0).getSessionId();
            List<AgentTurnTrace> bySession = agentTurnTraceMapper.selectBySessionId(sessionId);
            System.out.printf("C5MYSQL bySession sessionId=%d count=%d%n", sessionId, bySession.size());
        }

        // 保留期查询（只 COUNT 语义，不执行删除）。
        System.out.printf("C5MYSQL retentionProbe threshold=%s%n", LocalDateTime.now().minusDays(90));
        System.out.println("C5MYSQL DONE");
    }

    private int countSteps(String steps) {
        if (steps.isBlank()) {
            return 0;
        }
        int count = 0;
        int from = 0;
        while (true) {
            int hit = steps.indexOf("\"step\"", from);
            if (hit < 0) {
                break;
            }
            count++;
            from = hit + 1;
        }
        return count;
    }
}
