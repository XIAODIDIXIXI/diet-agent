# 饮食 Agent 评测报告

- 运行模式：live
- 数据来源：AI辅助构造与标注，未经过独立人工复核
- 数据库：diet_eval_874918eb47eb
- 运行时间：2026-10-03T16:00:47.272351800+08:00[Asia/Shanghai]
- 模型：qwen-turbo / qwen-max

| 指标 | 成功/总数 | 实测比例 |
|---|---:|---:|
| 规则修正后意图路由准确率 | 562/600 | 93.67% |
| 规则修正前意图准确率（含服务解析与兜底） | 587/600 | 97.83% |
| 端到端任务成功率 | 56/100 | 56.00% |
| 多轮槽位补全成功率 | 60/60 | 100.00% |
| 换一批餐食重复率（越低越好） | 0/0 | 未测 / 不适用 |
| 换新任务成功率 | 0/30 | 0.00% |
| 要求推荐时的非空响应率 | 86/130 | 66.15% |
| 计划轮次记录完整率 | 260/260 | 100.00% |
| 最终非空推荐卡片白名单合规率 | 86/86 | 100.00% |
| 预定义关键节点 Trace 覆盖率 | 3304/3304 | 100.00% |
| 故障注入降级可用率 | 0/0 | 未测 / 不适用 |

要求推荐但返回空卡片的已记录轮次：44；缺失轮次：0。

## 调用观测

- Agent调用数：1068。
- 可获得usage的调用：1068/1068；已报告Token总数：1452124。
- Agent耗时均值：1146.18 ms；P95：3840 ms。

## 解释与边界

- 未完成或缺失的意图/任务样本保留在分母；零分母报告未测。
- 空推荐不计为白名单成功；同时报告换新成功率，避免空结果掩盖无法推荐。
- 所有任务轮次均须满足标注验收条件，最后一轮成功不能覆盖前面失败。
- Trace 覆盖率基于评测前定义的场景节点清单；不是 Java 行覆盖率，也不代表所有 HTTP 入口。
- 若计划轮次未执行完，Trace覆盖率标为未测，不能用局部记录宣称完整覆盖。
- 故障注入使用受控 Agent 响应；不计入真实模型语义准确率。
- 合成样本包含模板变体，不能当作同等数量的独立真实用户样本或人工金标准。
- 不得将本报告等同于线上成功率或医学安全性验证。

## 失败样本

- `intent/meal_recommendation-19-2`：Expected MEAL_RECOMMENDATION, received HEALTH_RISK
- `intent/meal_recommendation-19-1`：Expected MEAL_RECOMMENDATION, received HEALTH_RISK
- `intent/meal_recommendation-19-3`：Expected MEAL_RECOMMENDATION, received HEALTH_RISK
- `intent/meal_recommendation-19-5`：Expected MEAL_RECOMMENDATION, received HEALTH_RISK
- `intent/meal_recommendation-19-4`：Expected MEAL_RECOMMENDATION, received HEALTH_RISK
- `intent/meal_recommendation-20-1`：Expected MEAL_RECOMMENDATION, received MEAL_PLAN
- `intent/meal_recommendation-20-2`：Expected MEAL_RECOMMENDATION, received MEAL_PLAN
- `intent/meal_recommendation-20-3`：Expected MEAL_RECOMMENDATION, received MEAL_PLAN
- `intent/meal_recommendation-20-4`：Expected MEAL_RECOMMENDATION, received MEAL_PLAN
- `intent/meal_recommendation-20-5`：Expected MEAL_RECOMMENDATION, received MEAL_PLAN
- `intent/clarify_needed-13-2`：Expected CLARIFY_NEEDED, received MEAL_RECOMMENDATION
- `intent/clarify_needed-16-1`：Expected CLARIFY_NEEDED, received MEAL_RECOMMENDATION
- `intent/clarify_needed-16-2`：Expected CLARIFY_NEEDED, received MEAL_RECOMMENDATION
- `intent/clarify_needed-16-3`：Expected CLARIFY_NEEDED, received MEAL_RECOMMENDATION
- `intent/meal_plan-03-2`：Expected MEAL_PLAN, received MEAL_RECOMMENDATION
- `intent/meal_plan-03-1`：Expected MEAL_PLAN, received MEAL_RECOMMENDATION
- `intent/meal_plan-14-2`：Expected MEAL_PLAN, received MEAL_RECOMMENDATION
- `intent/meal_plan-14-5`：Expected MEAL_PLAN, received MEAL_RECOMMENDATION
- `intent/meal_plan-15-1`：Expected MEAL_PLAN, received MEAL_RECOMMENDATION
- `intent/meal_plan-15-2`：Expected MEAL_PLAN, received MEAL_RECOMMENDATION
- `intent/meal_plan-15-3`：Expected MEAL_PLAN, received MEAL_RECOMMENDATION
- `intent/meal_plan-15-4`：Expected MEAL_PLAN, received MEAL_RECOMMENDATION
- `intent/meal_plan-15-5`：Expected MEAL_PLAN, received MEAL_RECOMMENDATION
- `intent/other-17-1`：Expected OTHER, received HEALTH_RISK
- `intent/other-17-2`：Expected OTHER, received HEALTH_RISK
- `intent/other-17-3`：Expected OTHER, received HEALTH_RISK
- `intent/other-17-4`：Expected OTHER, received HEALTH_RISK
- `intent/other-17-5`：Expected OTHER, received HEALTH_RISK
- `intent/other-18-1`：Expected OTHER, received HEALTH_RISK
- `intent/other-18-2`：Expected OTHER, received HEALTH_RISK
- `intent/other-18-3`：Expected OTHER, received HEALTH_RISK
- `intent/other-18-4`：Expected OTHER, received HEALTH_RISK
- `intent/other-18-5`：Expected OTHER, received HEALTH_RISK
- `intent/other-19-1`：Expected OTHER, received MEAL_PLAN
- `intent/other-19-2`：Expected OTHER, received MEAL_PLAN
- `intent/other-19-3`：Expected OTHER, received MEAL_PLAN
- `intent/other-19-4`：Expected OTHER, received MEAL_PLAN
- `intent/other-19-5`：Expected OTHER, received MEAL_PLAN
- `task/slot-005`：[['Insufficient recommendations']]
- `task/slot-006`：[['Insufficient recommendations']]
- `task/slot-007`：[['Insufficient recommendations']]
- `task/slot-008`：[['Insufficient recommendations']]
- `task/slot-009`：[['Insufficient recommendations']]
- `task/slot-010`：[['Insufficient recommendations']]
- `task/slot-011`：[['Insufficient recommendations']]
- `task/slot-012`：[['Insufficient recommendations']]
- `task/slot-013`：[['Insufficient recommendations']]
- `task/slot-014`：[['Insufficient recommendations']]
- `task/slot-015`：[['Insufficient recommendations']]
- `task/slot-016`：[['Insufficient recommendations']]
- `task/slot-032`：[['Insufficient recommendations']]
- `task/slot-036`：[['Insufficient recommendations']]
- `task/adjust-001`：[['Insufficient recommendations']]
- `task/adjust-002`：[['Insufficient recommendations']]
- `task/adjust-003`：[['Insufficient recommendations']]
- `task/adjust-004`：[['Insufficient recommendations']]
- `task/adjust-005`：[['Insufficient recommendations']]
- `task/adjust-006`：[['Insufficient recommendations']]
- `task/adjust-007`：[['Insufficient recommendations']]
- `task/adjust-008`：[['Insufficient recommendations']]
- `task/adjust-009`：[['Insufficient recommendations']]
- `task/adjust-010`：[['Insufficient recommendations']]
- `task/adjust-011`：[['Insufficient recommendations']]
- `task/adjust-012`：[['Insufficient recommendations']]
- `task/adjust-013`：[['Insufficient recommendations']]
- `task/adjust-014`：[['Insufficient recommendations']]
- `task/adjust-015`：[['Insufficient recommendations']]
- `task/adjust-016`：[['Insufficient recommendations']]
- `task/adjust-017`：[['Insufficient recommendations']]
- `task/adjust-018`：[['Insufficient recommendations']]
- `task/adjust-019`：[['Insufficient recommendations']]
- `task/adjust-020`：[['Insufficient recommendations']]
- `task/adjust-021`：[['Insufficient recommendations']]
- `task/adjust-022`：[['Insufficient recommendations']]
- `task/adjust-023`：[['Insufficient recommendations']]
- `task/adjust-024`：[['Insufficient recommendations']]
- `task/adjust-025`：[['Insufficient recommendations']]
- `task/adjust-026`：[['Insufficient recommendations']]
- `task/adjust-027`：[['Insufficient recommendations']]
- `task/adjust-028`：[['Insufficient recommendations']]
- `task/adjust-029`：[['Insufficient recommendations']]
- `task/adjust-030`：[['Insufficient recommendations']]
