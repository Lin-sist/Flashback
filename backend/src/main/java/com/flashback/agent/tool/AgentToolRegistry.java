package com.flashback.agent.tool;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.flashback.agent.tool.AgentToolSpec.AgentToolParameter;

/**
 * 工具白名单唯一事实源（C2）。
 *
 * 边界（design.md 关键不变量 2）：
 * - 下发给 provider 的 tools schema 由本 registry 派生，白名单与 prompt 无从漂移；
 * - 未注册的工具名一律按白名单外提议拒绝，不猜测、不补全；
 * - 读工具在此声明以保持白名单与审计的完整视图，但不下发为 FC tool（design §3.1）。
 */
@Component
public class AgentToolRegistry {

    /** askText 参数名，所有写工具共用。 */
    public static final String PARAM_ASK_TEXT = "askText";

    public static final String PARAM_TEXT = "text";
    public static final String PARAM_TAG_IDS = "tagIds";
    public static final String PARAM_UNLOCK_AT = "unlockAt";

    /**
     * unlockAt 期望形如 2026-08-01T09:30:00 的本地日期时间。
     * strict mode 支持 pattern，可把「形状」校验前移到 provider 服务端；
     * 「必须晚于当前时间」属业务边界，仍由 validator 与 RecordService 把关。
     */
    static final String UNLOCK_AT_PATTERN = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}(:\\d{2})?$";

    private final Map<AgentToolName, AgentToolSpec> specs;

    public AgentToolRegistry() {
        Map<AgentToolName, AgentToolSpec> declared = new LinkedHashMap<>();

        declared.put(AgentToolName.APPEND_RECORD_CONTENT, new AgentToolSpec(
                AgentToolName.APPEND_RECORD_CONTENT,
                "把这次对话中用户自己说过的内容整理成一小段，追加到他正在写的记录正文末尾。"
                        + "只能追加，不能改写或替换用户已经写下的文字。",
                List.of(
                        AgentToolParameter.string(PARAM_TEXT,
                                "要追加的素材，只能来自用户说过的内容，保留用户自己的措辞，不加入你的分析或评价。"),
                        askTextParam("你要对用户说的那句征询，例如“要不要把这段放进正文？”"))));

        declared.put(AgentToolName.ADD_RECORD_TAGS, new AgentToolSpec(
                AgentToolName.ADD_RECORD_TAGS,
                "在用户正在写的记录上追加标签。只能从已提供的可选标签中挑选，不能创建新标签，"
                        + "也不会移除用户已有的标签。",
                List.of(
                        AgentToolParameter.integerArray(PARAM_TAG_IDS,
                                "要追加的标签 id，必须来自上下文中给出的可选标签清单。"),
                        askTextParam("你要对用户说的那句征询，例如“要不要加个这样的标签？”"))));

        declared.put(AgentToolName.PROPOSE_UNLOCK_AT, new AgentToolSpec(
                AgentToolName.PROPOSE_UNLOCK_AT,
                "为用户正在写的记录设置一个解锁时间。这一步不会封存记录，封存仍然要由用户自己在页面上确认。",
                List.of(
                        AgentToolParameter.string(PARAM_UNLOCK_AT,
                                "解锁时间，形如 2026-08-01T09:30:00，必须晚于当前时间。",
                                UNLOCK_AT_PATTERN),
                        askTextParam("你要对用户说的那句征询，例如“要不要把它留到明年的今天再打开？”"))));

        declared.put(AgentToolName.LIST_AVAILABLE_TAGS, new AgentToolSpec(
                AgentToolName.LIST_AVAILABLE_TAGS,
                "可选标签清单。由后端在组装上下文时直接注入，模型不需要也不能调用它。",
                List.of()));

        declared.put(AgentToolName.READ_DRAFT_SNAPSHOT, new AgentToolSpec(
                AgentToolName.READ_DRAFT_SNAPSHOT,
                "当前草稿快照。由后端在组装上下文时直接注入，模型不需要也不能调用它。",
                List.of()));

        this.specs = Map.copyOf(declared);
    }

    private static AgentToolParameter askTextParam(String description) {
        return AgentToolParameter.string(PARAM_ASK_TEXT, description);
    }

    /**
     * 按枚举取规格。
     */
    public AgentToolSpec find(AgentToolName name) {
        return name == null ? null : specs.get(name);
    }

    /**
     * 按 wire name 取规格；未注册返回 null。
     */
    public AgentToolSpec findByWireName(String wireName) {
        return find(AgentToolName.fromWireName(wireName));
    }

    /**
     * 需要下发给 provider 的工具（仅写工具）。
     */
    public List<AgentToolSpec> functionCallingTools() {
        return specs.values().stream()
                .filter(spec -> spec.name().isWriteTool())
                .toList();
    }

    /**
     * 后端预注入的读工具（不下发给 provider）。
     */
    public List<AgentToolSpec> preInjectedReadTools() {
        return specs.values().stream()
                .filter(spec -> !spec.name().isWriteTool())
                .toList();
    }

    /**
     * 判断某个 wire name 是否为可被模型提议的工具。
     * 读工具不可被提议：它们不下发给 provider，模型提议它们即视为越界。
     */
    public boolean isProposable(String wireName) {
        AgentToolSpec spec = findByWireName(wireName);
        return spec != null && spec.name().isWriteTool();
    }
}
