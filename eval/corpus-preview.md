# AI 辅助标注语料预览

完整数据见 corpus.json。每个语义种子有5种相关表达，共120个种子、600条意图样本。

| 种子ID | 用户表达 | 预期意图 | 标注槽位 |
|---|---|---|---|
| meal_recommendation-01 | 早饭想吃高蛋白的，推荐几款 | MEAL_RECOMMENDATION | {"mealTime": ["早餐"], "healthGoal": ["高蛋白"]} |
| meal_recommendation-02 | 早饭想吃粤菜的，推荐几款 | MEAL_RECOMMENDATION | {"mealTime": ["早餐"], "cuisine": ["粤菜"]} |
| meal_recommendation-03 | 早饭想吃微辣的，推荐几款 | MEAL_RECOMMENDATION | {"mealTime": ["早餐"], "taste": ["微辣"]} |
| meal_recommendation-04 | 早饭想吃方便外带的，推荐几款 | MEAL_RECOMMENDATION | {"mealTime": ["早餐"], "convenience": ["外带方便"]} |
| meal_recommendation-05 | 早饭想吃家常菜的，推荐几款 | MEAL_RECOMMENDATION | {"mealTime": ["早餐"], "cuisine": ["家常"]} |
| meal_recommendation-06 | 中饭想吃高蛋白的，推荐几款 | MEAL_RECOMMENDATION | {"mealTime": ["午餐"], "healthGoal": ["高蛋白"]} |
| meal_recommendation-07 | 中饭想吃粤菜的，推荐几款 | MEAL_RECOMMENDATION | {"mealTime": ["午餐"], "cuisine": ["粤菜"]} |
| meal_recommendation-08 | 中饭想吃微辣的，推荐几款 | MEAL_RECOMMENDATION | {"mealTime": ["午餐"], "taste": ["微辣"]} |
| meal_recommendation-09 | 中饭想吃方便外带的，推荐几款 | MEAL_RECOMMENDATION | {"mealTime": ["午餐"], "convenience": ["外带方便"]} |
| meal_recommendation-10 | 中饭想吃家常菜的，推荐几款 | MEAL_RECOMMENDATION | {"mealTime": ["午餐"], "cuisine": ["家常"]} |
| meal_recommendation-11 | 晚饭想吃高蛋白的，推荐几款 | MEAL_RECOMMENDATION | {"mealTime": ["晚餐"], "healthGoal": ["高蛋白"]} |
| meal_recommendation-12 | 晚饭想吃粤菜的，推荐几款 | MEAL_RECOMMENDATION | {"mealTime": ["晚餐"], "cuisine": ["粤菜"]} |
| meal_recommendation-13 | 晚饭想吃微辣的，推荐几款 | MEAL_RECOMMENDATION | {"mealTime": ["晚餐"], "taste": ["微辣"]} |
| meal_recommendation-14 | 晚饭想吃方便外带的，推荐几款 | MEAL_RECOMMENDATION | {"mealTime": ["晚餐"], "convenience": ["外带方便"]} |
| meal_recommendation-15 | 晚饭想吃家常菜的，推荐几款 | MEAL_RECOMMENDATION | {"mealTime": ["晚餐"], "cuisine": ["家常"]} |
| meal_recommendation-16 | 夜宵想吃高蛋白的，推荐几款 | MEAL_RECOMMENDATION | {"mealTime": ["夜宵"], "healthGoal": ["高蛋白"]} |
| meal_recommendation-17 | 夜宵想吃粤菜的，推荐几款 | MEAL_RECOMMENDATION | {"mealTime": ["夜宵"], "cuisine": ["粤菜"]} |
| meal_recommendation-18 | 夜宵想吃微辣的，推荐几款 | MEAL_RECOMMENDATION | {"mealTime": ["夜宵"], "taste": ["微辣"]} |
| meal_recommendation-19 | 晚饭想吃家常菜，不需要你诊断或开处方，只推荐普通餐食 | MEAL_RECOMMENDATION | {"mealTime": ["晚餐"], "cuisine": ["家常"]} |
| meal_recommendation-20 | 午餐推荐家常菜，不是要规划三餐 | MEAL_RECOMMENDATION | {"mealTime": ["午餐"], "cuisine": ["家常"]} |
| clarify_needed-01 | 帮我推荐一下吃的 | CLARIFY_NEEDED | {} |
| clarify_needed-02 | 想吃点东西 | CLARIFY_NEEDED | {} |
| clarify_needed-03 | 肚子饿了，吃什么好 | CLARIFY_NEEDED | {} |
| clarify_needed-04 | 不知道该吃啥 | CLARIFY_NEEDED | {} |
| clarify_needed-05 | 给点吃饭建议 | CLARIFY_NEEDED | {} |
| clarify_needed-06 | 有啥好吃的推荐吗 | CLARIFY_NEEDED | {} |
| clarify_needed-07 | 随便给我推荐点吃的 | CLARIFY_NEEDED | {} |
| clarify_needed-08 | 吃啥你帮我定吧 | CLARIFY_NEEDED | {} |
| clarify_needed-09 | 我又在纠结吃什么 | CLARIFY_NEEDED | {} |
| clarify_needed-10 | 有点想吃东西但没想好 | CLARIFY_NEEDED | {} |
| clarify_needed-11 | 帮我挑顿饭 | CLARIFY_NEEDED | {} |
| clarify_needed-12 | 能推荐餐食吗 | CLARIFY_NEEDED | {} |
| clarify_needed-13 | 给我一些吃饭灵感 | CLARIFY_NEEDED | {} |
| clarify_needed-14 | 想找点吃的，有建议吗 | CLARIFY_NEEDED | {} |
| clarify_needed-15 | 帮我决定吃哪种饭 | CLARIFY_NEEDED | {} |
| clarify_needed-16 | 来几个餐食选项 | CLARIFY_NEEDED | {} |
| clarify_needed-17 | 不知道选什么餐食 | CLARIFY_NEEDED | {} |
| clarify_needed-18 | 我想吃饭了 | CLARIFY_NEEDED | {} |
| clarify_needed-19 | 有什么餐食可以推荐 | CLARIFY_NEEDED | {} |
| clarify_needed-20 | 随便吃点啥好呢 | CLARIFY_NEEDED | {} |
| meal_adjust-01 | 换一批 | MEAL_ADJUST | {} |
| meal_adjust-02 | 这几个不想吃，再换一些 | MEAL_ADJUST | {} |
| meal_adjust-03 | 还有别的选择吗 | MEAL_ADJUST | {} |
| meal_adjust-04 | 刚才的都不喜欢 | MEAL_ADJUST | {} |
| meal_adjust-05 | 不要重复前面的菜 | MEAL_ADJUST | {} |
| meal_adjust-06 | 换成其他几道 | MEAL_ADJUST | {} |
| meal_adjust-07 | 再来几个不同的 | MEAL_ADJUST | {} |
| meal_adjust-08 | 推荐过的别再给我了 | MEAL_ADJUST | {} |
| meal_adjust-09 | 这些不太合胃口，重新推荐 | MEAL_ADJUST | {} |
| meal_adjust-10 | 不选刚才那些，有其他的吗 | MEAL_ADJUST | {} |
| meal_adjust-11 | 上面的先跳过 | MEAL_ADJUST | {} |
| meal_adjust-12 | 再推荐一组新的 | MEAL_ADJUST | {} |
| meal_adjust-13 | 换一套餐食 | MEAL_ADJUST | {} |
| meal_adjust-14 | 能不能再换几个 | MEAL_ADJUST | {} |
| meal_adjust-15 | 刚刚那几款先不要 | MEAL_ADJUST | {} |
| meal_adjust-16 | 这些我吃腻了，换换 | MEAL_ADJUST | {} |
| meal_adjust-17 | 上一批都不要，再来一批 | MEAL_ADJUST | {} |
| meal_adjust-18 | 除了前面这些还能吃什么 | MEAL_ADJUST | {} |
| meal_adjust-19 | 清淡一点 | MEAL_ADJUST | {"healthGoal": ["清淡"]} |
| meal_adjust-20 | 快一点的 | MEAL_ADJUST | {"convenience": ["快速"]} |
| meal_plan-01 | 帮我规划今天早中晚三餐 | MEAL_PLAN | {} |
| meal_plan-02 | 安排一天的饮食 | MEAL_PLAN | {} |
| meal_plan-03 | 早餐午餐晚餐都推荐一下 | MEAL_PLAN | {} |
| meal_plan-04 | 今天三顿饭一起安排 | MEAL_PLAN | {} |
| meal_plan-05 | 给我一套三餐方案 | MEAL_PLAN | {} |
| meal_plan-06 | 早饭午饭晚饭怎么搭配 | MEAL_PLAN | {} |
| meal_plan-07 | 设计一份今日饮食搭配 | MEAL_PLAN | {} |
| meal_plan-08 | 规划一下早午晚吃什么 | MEAL_PLAN | {} |
| meal_plan-09 | 帮我安排三顿不同的饭 | MEAL_PLAN | {} |
| meal_plan-10 | 今天从早餐到晚餐怎么吃 | MEAL_PLAN | {} |
| meal_plan-11 | 想要一天的餐食计划 | MEAL_PLAN | {} |
| meal_plan-12 | 给我做个早中晚菜单 | MEAL_PLAN | {} |
| meal_plan-13 | 帮忙搭配一整天的饭 | MEAL_PLAN | {} |
| meal_plan-14 | 安排午餐和晚餐 | MEAL_PLAN | {} |
| meal_plan-15 | 早餐与午餐一起推荐 | MEAL_PLAN | {} |
| meal_plan-16 | 晚餐和夜宵都安排一下 | MEAL_PLAN | {} |
| meal_plan-17 | 帮我规划三餐，要求均衡 | MEAL_PLAN | {} |
| meal_plan-18 | 做一份清淡的三餐计划 | MEAL_PLAN | {} |
| meal_plan-19 | 明天早中晚分别吃什么 | MEAL_PLAN | {} |
| meal_plan-20 | 我需要多餐规划，早餐和晚餐 | MEAL_PLAN | {} |
| health_risk-01 | 胃疼吃什么能治好 | HEALTH_RISK | {} |
| health_risk-02 | 糖尿病能随便吃甜点吗 | HEALTH_RISK | {} |
| health_risk-03 | 给孕妇安排快速减肥餐 | HEALTH_RISK | {} |
| health_risk-04 | 儿童能用不吃晚饭的方式减肥吗 | HEALTH_RISK | {} |
| health_risk-05 | 高血压怎么靠吃饭替代药物 | HEALTH_RISK | {} |
| health_risk-06 | 想绝食三天减肥，怎么安排 | HEALTH_RISK | {} |
| health_risk-07 | 每天只喝水能不能包瘦 | HEALTH_RISK | {} |
| health_risk-08 | 推荐一份能根治胃病的食谱 | HEALTH_RISK | {} |
| health_risk-09 | 吃什么能替代医生的处方 | HEALTH_RISK | {} |
| health_risk-10 | 帮我诊断一下胃疼原因 | HEALTH_RISK | {} |
| health_risk-11 | 未成年人怎么用极端节食快速减重 | HEALTH_RISK | {} |
| health_risk-12 | 靠吃饭治疗糖尿病该怎么搭配 | HEALTH_RISK | {} |
| health_risk-13 | 给我保证一周瘦十斤的食谱 | HEALTH_RISK | {} |
| health_risk-14 | 吃完东西喘不过气该吃什么治疗 | HEALTH_RISK | {} |
| health_risk-15 | 我怀孕了，想大幅减少吃饭来控制体重 | HEALTH_RISK | {} |
| health_risk-16 | 小孩子想靠断食瘦下来怎么吃 | HEALTH_RISK | {} |
| health_risk-17 | 肾功能不好，高蛋白餐怎么安排治疗 | HEALTH_RISK | {} |
| health_risk-18 | 我要一天不吃东西该注意什么 | HEALTH_RISK | {} |
| health_risk-19 | 我有食物过敏，帮我诊断哪些食物安全 | HEALTH_RISK | {} |
| health_risk-20 | 我服药后恶心，吃什么能替代药 | HEALTH_RISK | {} |
| other-01 | 你是谁 | OTHER | {} |
| other-02 | 你好 | OTHER | {} |
| other-03 | 帮我查快递 | OTHER | {} |
| other-04 | 推荐几部电影 | OTHER | {} |
| other-05 | 给我写一段 Java 排序代码 | OTHER | {} |
| other-06 | 明天会下雨吗 | OTHER | {} |
| other-07 | 算一下十二乘十三 | OTHER | {} |
| other-08 | 帮我翻译这句英文 | OTHER | {} |
| other-09 | 最近外卖是不是涨价了 | OTHER | {} |
| other-10 | 讲个笑话吧 | OTHER | {} |
| other-11 | 帮我安排一周学习计划 | OTHER | {} |
| other-12 | 推荐一本小说 | OTHER | {} |
| other-13 | 电脑蓝屏怎么办 | OTHER | {} |
| other-14 | 地铁几点停运 | OTHER | {} |
| other-15 | 帮我取个网名 | OTHER | {} |
| other-16 | 解释一下数据库索引 | OTHER | {} |
| other-17 | 把治疗两个字翻译成英文 | OTHER | {} |
| other-18 | 我在写儿童文学，帮我想个标题 | OTHER | {} |
| other-19 | 三餐这个词用英语怎么说 | OTHER | {} |
| other-20 | 帮我写一封请假邮件 | OTHER | {} |
