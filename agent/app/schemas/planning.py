# 声明规划输入输出结构，以及字段类型、默认值和合法范围。
# 规划快照契约；新建引用和既有对象引用明确区分，拒绝无法执行的空方案。

# 从 datetime 引入 日期（date）、时间与耗时工具库（time）。
from datetime import date, time

# 从 typing 引入 给类型附加校验或依赖说明的类型标记（Annotated）。
from typing import Annotated

# 从 zoneinfo 引入 时区信息（ZoneInfo）、时区信息NotFound错误（ZoneInfoNotFoundError）。
from zoneinfo import ZoneInfo, ZoneInfoNotFoundError

# 从 pydantic 引入 基础模型（BaseModel）、ConfigDict、字段（Field）、文字Constraints（StringConstraints）、模型validator（model_validator）。
from pydantic import BaseModel, ConfigDict, Field, StringConstraints, model_validator

# 把依次包含 固定状态或编码 GOAL_PLAN（目标方案）、固定状态或编码 STAGE_PLAN（阶段方案）、固定状态或编码 TASK_PLAN（任务方案）、固定状态或编码 WEEKLY_PLAN（每周方案）、固定状态或编码 DAILY_PLAN（每日方案）等内容 的集合保存到 INTENTS。
INTENTS = (
    # 继续传入或组合固定状态或编码 GOAL_PLAN（目标方案），完成上面尚未结束的表达式。
    "GOAL_PLAN",
    # 继续传入或组合固定状态或编码 STAGE_PLAN（阶段方案），完成上面尚未结束的表达式。
    "STAGE_PLAN",
    # 继续传入或组合固定状态或编码 TASK_PLAN（任务方案），完成上面尚未结束的表达式。
    "TASK_PLAN",
    # 继续传入或组合固定状态或编码 WEEKLY_PLAN（每周方案），完成上面尚未结束的表达式。
    "WEEKLY_PLAN",
    # 继续传入或组合固定状态或编码 DAILY_PLAN（每日方案），完成上面尚未结束的表达式。
    "DAILY_PLAN",
    # 继续传入或组合固定状态或编码 HABIT_PLAN（习惯方案），完成上面尚未结束的表达式。
    "HABIT_PLAN",
    # 继续传入或组合固定状态或编码 COMPREHENSIVE_PLAN（综合方案），完成上面尚未结束的表达式。
    "COMPREHENSIVE_PLAN",
)
# 把从给类型附加校验或依赖说明的类型标记（Annotated）中按依次包含 str、执行“调用 StringConstraints 完成文字Constraints处理”得到的结果 的集合取出的内容保存到 编号（EntityId）。
EntityId = Annotated[str, StringConstraints(pattern=r"^[1-9][0-9]{0,18}$")]


# 定义 PlanModel，把方案模型需要的数据与操作放在同一个对象中。
class PlanModel(BaseModel):
    # 把执行“调用 ConfigDict 完成ConfigDict处理”得到的结果保存到 模型config（model_config）。
    model_config = ConfigDict(extra="forbid", str_strip_whitespace=True)


# 定义 PlanGoal，把方案目标需要的数据与操作放在同一个对象中。
class PlanGoal(PlanModel):
    # 声明 草案中的临时关联编号（clientKey），类型为 str；其内容为执行“调用 Field 完成字段处理”得到的结果。
    clientKey: str = Field(default="g1", min_length=1, max_length=64)
    # 声明 标题（title），类型为 str；其内容为执行“调用 Field 完成字段处理”得到的结果。
    title: str = Field(min_length=1, max_length=100)
    # 声明 说明（description），类型为 str | None；其内容为执行“调用 Field 完成字段处理”得到的结果。
    description: str | None = Field(default=None, max_length=2000)
    # 声明 完成情况判断标准（completionCriteria），类型为 str | None；其内容为执行“调用 Field 完成字段处理”得到的结果。
    completionCriteria: str | None = Field(default=None, max_length=2000)
    # 声明 开始日期（startDate），类型为 date | None；其内容为空值，表示当前还没有对应内容。
    startDate: date | None = None
    # 声明 截止日期（dueDate），类型为 date | None；其内容为空值，表示当前还没有对应内容。
    dueDate: date | None = None


# 定义 PlanStage，把方案阶段需要的数据与操作放在同一个对象中。
class PlanStage(PlanGoal):
    # 声明 草案中的临时关联编号（clientKey），类型为 str；其内容为执行“调用 Field 完成字段处理”得到的结果。
    clientKey: str = Field(min_length=1, max_length=64)
    # 声明 目标键（goalKey），类型为 str | None；其内容为空值，表示当前还没有对应内容。
    goalKey: str | None = None
    # 声明 目标编号（goalId），类型为 EntityId | None；其内容为空值，表示当前还没有对应内容。
    goalId: EntityId | None = None
    # 声明 目标版本号（goalVersion），类型为 int | None；其内容为执行“调用 Field 完成字段处理”得到的结果。
    goalVersion: int | None = Field(default=None, ge=0)
    # 声明 排序顺序（sortOrder），类型为 int | None；其内容为执行“调用 Field 完成字段处理”得到的结果。
    sortOrder: int | None = Field(default=None, ge=0, le=9999)


# 定义 PlanTask，把方案任务需要的数据与操作放在同一个对象中。
class PlanTask(PlanGoal):
    # 声明 草案中的临时关联编号（clientKey），类型为 str；其内容为执行“调用 Field 完成字段处理”得到的结果。
    clientKey: str = Field(min_length=1, max_length=64)
    # 声明 阶段键（stageKey），类型为 str | None；其内容为空值，表示当前还没有对应内容。
    stageKey: str | None = None
    # 声明 阶段编号（stageId），类型为 EntityId | None；其内容为空值，表示当前还没有对应内容。
    stageId: EntityId | None = None
    # 声明 阶段版本号（stageVersion），类型为 int | None；其内容为执行“调用 Field 完成字段处理”得到的结果。
    stageVersion: int | None = Field(default=None, ge=0)
    # 声明 priority，类型为 str | None；其内容为执行“调用 Field 完成字段处理”得到的结果。
    priority: str | None = Field(default=None, pattern="^(LOW|MEDIUM|HIGH|URGENT)$")
    # 声明 权重（weight），类型为 int | None；其内容为执行“调用 Field 完成字段处理”得到的结果。
    weight: int | None = Field(default=None, ge=1)
    # 声明 estimated分钟数（estimatedMinutes），类型为 int | None；其内容为执行“调用 Field 完成字段处理”得到的结果。
    estimatedMinutes: int | None = Field(default=None, ge=1, le=1440)
    # 声明 排序顺序（sortOrder），类型为 int | None；其内容为执行“调用 Field 完成字段处理”得到的结果。
    sortOrder: int | None = Field(default=None, ge=0, le=9999)


# 定义 PlanWeekly，把方案每周需要的数据与操作放在同一个对象中。
class PlanWeekly(PlanModel):
    # 声明 草案中的临时关联编号（clientKey），类型为 str；其内容为执行“调用 Field 完成字段处理”得到的结果。
    clientKey: str = Field(min_length=1, max_length=64)
    # 声明 周开始（weekStart），类型为 date。
    weekStart: date


# 定义 PlanDaily，把方案每日需要的数据与操作放在同一个对象中。
class PlanDaily(PlanModel):
    # 声明 草案中的临时关联编号（clientKey），类型为 str；其内容为执行“调用 Field 完成字段处理”得到的结果。
    clientKey: str = Field(min_length=1, max_length=64)
    # 声明 周键（weekKey），类型为 str。
    weekKey: str
    # 声明 按用户时区计算的业务日期（businessDate），类型为 date。
    businessDate: date


# 定义 PlanAssignment，把方案任务安排需要的数据与操作放在同一个对象中。
class PlanAssignment(PlanModel):
    # 声明 草案中的临时关联编号（clientKey），类型为 str；其内容为执行“调用 Field 完成字段处理”得到的结果。
    clientKey: str = Field(min_length=1, max_length=64)
    # 声明 任务键（taskKey），类型为 str | None；其内容为空值，表示当前还没有对应内容。
    taskKey: str | None = None
    # 声明 任务编号（taskId），类型为 EntityId | None；其内容为空值，表示当前还没有对应内容。
    taskId: EntityId | None = None
    # 声明 任务版本号（taskVersion），类型为 int | None；其内容为执行“调用 Field 完成字段处理”得到的结果。
    taskVersion: int | None = Field(default=None, ge=0)
    # 声明 任务安排编号（assignmentId），类型为 EntityId | None；其内容为空值，表示当前还没有对应内容。
    assignmentId: EntityId | None = None
    # 声明 任务安排版本号（assignmentVersion），类型为 int | None；其内容为执行“调用 Field 完成字段处理”得到的结果。
    assignmentVersion: int | None = Field(default=None, ge=0)
    # 声明 周键（weekKey），类型为 str。
    weekKey: str
    # 声明 按用户时区计算的业务日期（businessDate），类型为 date | None；其内容为空值，表示当前还没有对应内容。
    businessDate: date | None = None
    # 声明 排序顺序（sortOrder），类型为 int | None；其内容为执行“调用 Field 完成字段处理”得到的结果。
    sortOrder: int | None = Field(default=None, ge=0, le=9999)
    # 声明 任务标题（taskTitle），类型为 str | None；其内容为执行“调用 Field 完成字段处理”得到的结果。
    taskTitle: str | None = Field(default=None, max_length=100)
    # 声明 上一次周开始（previousWeekStart），类型为 date | None；其内容为空值，表示当前还没有对应内容。
    previousWeekStart: date | None = None
    # 声明 上一次Business日期（previousBusinessDate），类型为 date | None；其内容为空值，表示当前还没有对应内容。
    previousBusinessDate: date | None = None
    # 声明 原因（reason），类型为 str | None；其内容为执行“调用 Field 完成字段处理”得到的结果。
    reason: str | None = Field(default=None, max_length=300)


# 定义 PlanHabit，把方案习惯需要的数据与操作放在同一个对象中。
class PlanHabit(PlanModel):
    # 声明 草案中的临时关联编号（clientKey），类型为 str；其内容为执行“调用 Field 完成字段处理”得到的结果。
    clientKey: str = Field(min_length=1, max_length=64)
    # 声明 名称（name），类型为 str；其内容为执行“调用 Field 完成字段处理”得到的结果。
    name: str = Field(min_length=1, max_length=100)
    # 声明 说明（description），类型为 str | None；其内容为执行“调用 Field 完成字段处理”得到的结果。
    description: str | None = Field(default=None, max_length=2000)
    # 声明 完成情况判断标准（completionCriteria），类型为 str | None；其内容为执行“调用 Field 完成字段处理”得到的结果。
    completionCriteria: str | None = Field(default=None, max_length=2000)
    # 声明 分类名称（categoryName），类型为 str | None；其内容为执行“调用 Field 完成字段处理”得到的结果。
    categoryName: str | None = Field(default=None, max_length=100)
    # 声明 cycle类型（cycleType），类型为 str；其内容为执行“调用 Field 完成字段处理”得到的结果。
    cycleType: str = Field(default="DAILY", pattern="^(DAILY|WEEKLY_DAYS)$")
    # 声明 weekdays，类型为 list[int] | None；其内容为执行“调用 Field 完成字段处理”得到的结果。
    weekdays: list[int] | None = Field(default=None, max_length=7)
    # 声明 开始日期（startDate），类型为 date | None；其内容为空值，表示当前还没有对应内容。
    startDate: date | None = None
    # 声明 预计时间（expectedTime），类型为 time | None；其内容为空值，表示当前还没有对应内容。
    expectedTime: time | None = None


# 定义 PlanSnapshot，把方案确认时的数据快照需要的数据与操作放在同一个对象中。
class PlanSnapshot(PlanModel):
    # 声明 intent，类型为 str。
    intent: str
    # 声明 用户使用的时区（timeZone），类型为 str | None；其内容为空值，表示当前还没有对应内容。
    timeZone: str | None = None
    # 声明 目标（goal），类型为 PlanGoal | None；其内容为空值，表示当前还没有对应内容。
    goal: PlanGoal | None = None
    # 声明 阶段列表（stages），类型为 list[PlanStage]；其内容为执行“调用 Field 完成字段处理”得到的结果。
    stages: list[PlanStage] = Field(default_factory=list, max_length=100)
    # 声明 任务列表（tasks），类型为 list[PlanTask]；其内容为执行“调用 Field 完成字段处理”得到的结果。
    tasks: list[PlanTask] = Field(default_factory=list, max_length=500)
    # 声明 每周方案列表（weeklyPlans），类型为 list[PlanWeekly]；其内容为执行“调用 Field 完成字段处理”得到的结果。
    weeklyPlans: list[PlanWeekly] = Field(default_factory=list, max_length=100)
    # 声明 每日方案列表（dailyPlans），类型为 list[PlanDaily]；其内容为执行“调用 Field 完成字段处理”得到的结果。
    dailyPlans: list[PlanDaily] = Field(default_factory=list, max_length=700)
    # 声明 任务任务安排列表（taskAssignments），类型为 list[PlanAssignment]；其内容为执行“调用 Field 完成字段处理”得到的结果。
    taskAssignments: list[PlanAssignment] = Field(default_factory=list, max_length=500)
    # 声明 习惯列表（habits），类型为 list[PlanHabit]；其内容为执行“调用 Field 完成字段处理”得到的结果。
    habits: list[PlanHabit] = Field(default_factory=list, max_length=100)

    # 在整组字段已经收集后检查组合关系，防止单项合法但组合不合理。
    @model_validator(mode="after")
    # 函数 validate_relations：检查 relations。
    def validate_relations(self) -> "PlanSnapshot":
        # 检查条件 self.intent not in INTENTS 是否成立；成立时执行缩进的分支。
        if self.intent not in INTENTS:
            # 中止本轮正常处理，抛出 执行“调用 ValueError 完成输入值不合法的异常处理”得到的结果。
            raise ValueError("方案意图无效")
        # 检查条件 self.timeZone 是否成立；成立时执行缩进的分支。
        if self.timeZone:
            # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
            try:
                # 调用 ZoneInfo 完成时区信息处理。
                ZoneInfo(self.timeZone)
            # 捕获(ZoneInfoNotFoundError, ValueError)异常，执行下面的错误处理，异常对象保存为 error。
            except (ZoneInfoNotFoundError, ValueError) as error:
                # 中止本轮正常处理，抛出 执行“调用 ValueError 完成输入值不合法的异常处理”得到的结果。
                raise ValueError("账号时区无效") from error
        # 检查条件 not any((self.goal, self.stages, self.tasks, self.weeklyPlans, self.dailyPlans, self.taskAssignments, self.habits)) 是否成立；成立时执行缩进的分支。
        if not any(
            (
                # 继续传入或组合表达式 self.goal 的结果，完成上面尚未结束的表达式。
                self.goal,
                # 继续传入或组合表达式 self.stages 的结果，完成上面尚未结束的表达式。
                self.stages,
                # 继续传入或组合表达式 self.tasks 的结果，完成上面尚未结束的表达式。
                self.tasks,
                # 继续传入或组合表达式 self.weeklyPlans 的结果，完成上面尚未结束的表达式。
                self.weeklyPlans,
                # 继续传入或组合表达式 self.dailyPlans 的结果，完成上面尚未结束的表达式。
                self.dailyPlans,
                # 继续传入或组合表达式 self.taskAssignments 的结果，完成上面尚未结束的表达式。
                self.taskAssignments,
                # 继续传入或组合表达式 self.habits 的结果，完成上面尚未结束的表达式。
                self.habits,
            )
        ):
            # 中止本轮正常处理，抛出 执行“调用 ValueError 完成输入值不合法的异常处理”得到的结果。
            raise ValueError("规划方案不能为空")
        # 声明 键列表（keys），类型为 set[str]；其内容为执行“调用 set 完成设置处理”得到的结果。
        keys: set[str] = set()
        # 把按 ([self.goal] if self.goal else []) + self.stages + self.tasks 计算的结果保存到 entities。
        entities = ([self.goal] if self.goal else []) + self.stages + self.tasks
        # 从entities依次取出entity，每项执行一次下面的处理。
        for entity in entities:
            # 检查条件 entity.startDate and entity.dueDate and (entity.startDate > entity.dueDate) 是否成立；成立时执行缩进的分支。
            if entity.startDate and entity.dueDate and entity.startDate > entity.dueDate:
                # 中止本轮正常处理，抛出 执行“调用 ValueError 完成输入值不合法的异常处理”得到的结果。
                raise ValueError("开始日期不能晚于截止日期")
        # 从按 entities + self.weeklyPlans + self.dailyPlans + self.taskAssignments + self.habits 计算的结果依次取出entity，每项执行一次下面的处理。
        for entity in (
            # 继续传入或组合按表达式计算得到的值：entities + self.weeklyPlans + self.dailyPlans + self.taskAssignments + self.habits，完成上面尚未结束的表达式。
            entities + self.weeklyPlans + self.dailyPlans + self.taskAssignments + self.habits
        ):
            # 检查条件 entity.clientKey in keys 是否成立；成立时执行缩进的分支。
            if entity.clientKey in keys:
                # 中止本轮正常处理，抛出 执行“调用 ValueError 完成输入值不合法的异常处理”得到的结果。
                raise ValueError("方案 clientKey 重复")
            # 加入集合或登记一条待保存记录。
            keys.add(entity.clientKey)
        # 把逐项处理数据，并按这里的条件筛选或转换后收集结果保存到 阶段键列表（stage_keys）。
        stage_keys = {stage.clientKey for stage in self.stages}
        # 把逐项处理数据，并按这里的条件筛选或转换后收集结果保存到 任务键列表（task_keys）。
        task_keys = {task.clientKey for task in self.tasks}
        # 把逐项处理数据，并按这里的条件筛选或转换后收集结果保存到 周计划列表（weeks）。
        weeks = {week.clientKey: week for week in self.weeklyPlans}
        # 从当前实例（self）的阶段列表（stages）属性依次取出阶段（stage），每项执行一次下面的处理。
        for stage in self.stages:
            # 把条件 self.goal is not None 是否成立并且stage.goalId is None，即对应内容是否不存在保存到 新上级（new_parent）。
            new_parent = self.goal is not None and stage.goalId is None
            # 检查条件 new_parent 是否成立；成立时执行缩进的分支。
            if new_parent:
                # 检查条件 stage.goalKey not in (None, self.goal.clientKey) 是否成立；成立时执行缩进的分支。
                if stage.goalKey not in (None, self.goal.clientKey):
                    # 中止本轮正常处理，抛出 执行“调用 ValueError 完成输入值不合法的异常处理”得到的结果。
                    raise ValueError("阶段目标引用无效")
            # 检查当前记录版本是否符合请求要求：stage.goalId is None or stage.goalVersion is None or stage.goalKey is not None；成立时执行缩进的分支。
            elif stage.goalId is None or stage.goalVersion is None or stage.goalKey is not None:
                # 中止本轮正常处理，抛出 执行“调用 ValueError 完成输入值不合法的异常处理”得到的结果。
                raise ValueError("已有目标引用必须携带 goalId 和 goalVersion")
        # 从当前实例（self）的任务列表（tasks）属性依次取出任务（task），每项执行一次下面的处理。
        for task in self.tasks:
            # 检查task.stageKey is None) == (task.stageId is None，即对应内容是否不存在；成立时执行缩进的分支。
            if (task.stageKey is None) == (task.stageId is None):
                # 中止本轮正常处理，抛出 执行“调用 ValueError 完成输入值不合法的异常处理”得到的结果。
                raise ValueError("任务必须且只能引用新阶段或已有阶段")
            # 检查条件 task.stageKey is not None and task.stageKey not in stage_keys 是否成立；成立时执行缩进的分支。
            if task.stageKey is not None and task.stageKey not in stage_keys:
                # 中止本轮正常处理，抛出 执行“调用 ValueError 完成输入值不合法的异常处理”得到的结果。
                raise ValueError("任务阶段引用无效")
            # 检查当前记录版本是否符合请求要求：task.stageId is not None and task.stageVersion is None；成立时执行缩进的分支。
            if task.stageId is not None and task.stageVersion is None:
                # 中止本轮正常处理，抛出 执行“调用 ValueError 完成输入值不合法的异常处理”得到的结果。
                raise ValueError("已有阶段引用必须携带 stageVersion")
        # 检查条件 len({week.weekStart for week in self.weeklyPlans}) != len(weeks) 是否成立；成立时执行缩进的分支。
        if len({week.weekStart for week in self.weeklyPlans}) != len(weeks):
            # 中止本轮正常处理，抛出 执行“调用 ValueError 完成输入值不合法的异常处理”得到的结果。
            raise ValueError("同一周不能重复定义")
        # 检查条件 any((week.weekStart.weekday() != 0 for week in self.weeklyPlans)) 是否成立；成立时执行缩进的分支。
        if any(week.weekStart.weekday() != 0 for week in self.weeklyPlans):
            # 中止本轮正常处理，抛出 执行“调用 ValueError 完成输入值不合法的异常处理”得到的结果。
            raise ValueError("weekStart 必须是自然周周一")
        # 从当前实例（self）的每日方案列表（dailyPlans）属性依次取出日（day），每项执行一次下面的处理。
        for day in self.dailyPlans:
            # 调用 self._validate_day 完成检查合法性日处理。
            self._validate_day(weeks, day.weekKey, day.businessDate)
        # 声明 assigned，类型为 set[str]；其内容为执行“调用 set 完成设置处理”得到的结果。
        assigned: set[str] = set()
        # 从当前实例（self）的任务任务安排列表（taskAssignments）属性依次取出任务安排（assignment），每项执行一次下面的处理。
        for assignment in self.taskAssignments:
            # 检查assignment.taskKey is None) == (assignment.taskId is None，即对应内容是否不存在；成立时执行缩进的分支。
            if (assignment.taskKey is None) == (assignment.taskId is None):
                # 中止本轮正常处理，抛出 执行“调用 ValueError 完成输入值不合法的异常处理”得到的结果。
                raise ValueError("安排必须且只能引用新任务或已有任务")
            # 检查条件 assignment.taskKey is not None and assignment.taskKey not in task_keys 是否成立；成立时执行缩进的分支。
            if assignment.taskKey is not None and assignment.taskKey not in task_keys:
                # 中止本轮正常处理，抛出 执行“调用 ValueError 完成输入值不合法的异常处理”得到的结果。
                raise ValueError("安排任务引用无效")
            # 检查当前记录版本是否符合请求要求：assignment.taskId is not None and assignment.taskVersion is None；成立时执行缩进的分支。
            if assignment.taskId is not None and assignment.taskVersion is None:
                # 中止本轮正常处理，抛出 执行“调用 ValueError 完成输入值不合法的异常处理”得到的结果。
                raise ValueError("已有任务安排必须携带 taskVersion")
            # 检查当前记录版本是否符合请求要求：assignment.assignmentId is None) != (assignment.assignmentVersion is None；成立时执行缩进的分支。
            if (assignment.assignmentId is None) != (assignment.assignmentVersion is None):
                # 中止本轮正常处理，抛出 执行“调用 ValueError 完成输入值不合法的异常处理”得到的结果。
                raise ValueError("原安排标识与版本必须同时提供")
            # 把任务安排（assignment）的任务键（taskKey）属性或者按 'existing:' + str(assignment.taskId) 计算的结果保存到 账户管理（identity）。
            identity = assignment.taskKey or "existing:" + str(assignment.taskId)
            # 检查条件 identity in assigned 是否成立；成立时执行缩进的分支。
            if identity in assigned:
                # 中止本轮正常处理，抛出 执行“调用 ValueError 完成输入值不合法的异常处理”得到的结果。
                raise ValueError("同一任务不能重复安排")
            # 加入集合或登记一条待保存记录。
            assigned.add(identity)
            # 检查条件 assignment.weekKey not in weeks 是否成立；成立时执行缩进的分支。
            if assignment.weekKey not in weeks:
                # 中止本轮正常处理，抛出 执行“调用 ValueError 完成输入值不合法的异常处理”得到的结果。
                raise ValueError("安排周计划引用无效")
            # 检查条件 assignment.businessDate 是否成立；成立时执行缩进的分支。
            if assignment.businessDate:
                # 调用 self._validate_day 完成检查合法性日处理。
                self._validate_day(weeks, assignment.weekKey, assignment.businessDate)
        # 从当前实例（self）的习惯列表（habits）属性依次取出习惯（habit），每项执行一次下面的处理。
        for habit in self.habits:
            # 检查条件 habit.weekdays and (len(set(habit.weekdays)) != len(habit.weekdays) or any((type(day) is not int or day < 1 or day > 7 for day in habit.weekdays)… 是否成立；成立时执行缩进的分支。
            if habit.weekdays and (
                # 继续传入或组合集合或文字的长度，完成上面尚未结束的表达式。
                len(set(habit.weekdays)) != len(habit.weekdays)
                # 继续补充本次调用或判断：优先使用前面的有效值，没有时采用后面的备用值：or any(type(day) is not int or day < 1 or day > 7 for day in habit.weekdays)。
                or any(type(day) is not int or day < 1 or day > 7 for day in habit.weekdays)
            ):
                # 中止本轮正常处理，抛出 执行“调用 ValueError 完成输入值不合法的异常处理”得到的结果。
                raise ValueError("星期必须是互不重复的 1 至 7")
            # 检查条件 habit.cycleType == 'WEEKLY_DAYS' and (not habit.weekdays) 是否成立；成立时执行缩进的分支。
            if habit.cycleType == "WEEKLY_DAYS" and not habit.weekdays:
                # 中止本轮正常处理，抛出 执行“调用 ValueError 完成输入值不合法的异常处理”得到的结果。
                raise ValueError("指定星期习惯必须提供 weekdays")
        # 检查self.intent == 'GOAL_PLAN' and self.goal is None，即对应内容是否不存在；成立时执行缩进的分支。
        if self.intent == "GOAL_PLAN" and self.goal is None:
            # 中止本轮正常处理，抛出 执行“调用 ValueError 完成输入值不合法的异常处理”得到的结果。
            raise ValueError("目标规划必须包含目标")
        # 检查条件 self.intent == 'STAGE_PLAN' and (self.goal is not None or not self.stages) 是否成立；成立时执行缩进的分支。
        if self.intent == "STAGE_PLAN" and (self.goal is not None or not self.stages):
            # 中止本轮正常处理，抛出 执行“调用 ValueError 完成输入值不合法的异常处理”得到的结果。
            raise ValueError("阶段规划应在已有目标下创建阶段")
        # 检查条件 self.intent == 'TASK_PLAN' and (self.goal is not None or self.stages or (not self.tasks)) 是否成立；成立时执行缩进的分支。
        if self.intent == "TASK_PLAN" and (self.goal is not None or self.stages or not self.tasks):
            # 中止本轮正常处理，抛出 执行“调用 ValueError 完成输入值不合法的异常处理”得到的结果。
            raise ValueError("任务规划应在已有阶段下创建任务")
        # 检查条件 self.intent in ('GOAL_PLAN', 'STAGE_PLAN', 'TASK_PLAN') and any((self.weeklyPlans, self.dailyPlans, self.taskAssignments, self.habits)) 是否成立；成立时执行缩进的分支。
        if self.intent in ("GOAL_PLAN", "STAGE_PLAN", "TASK_PLAN") and any(
            # 继续传入或组合表达式 (self.weeklyPlans, self.dailyPlans, self.taskAssignments, self.habits) 的结果，完成上面尚未结束的表达式。
            (self.weeklyPlans, self.dailyPlans, self.taskAssignments, self.habits)
        ):
            # 中止本轮正常处理，抛出 执行“调用 ValueError 完成输入值不合法的异常处理”得到的结果。
            raise ValueError("当前规划意图不包含计划或习惯")
        # 检查条件 self.intent in ('WEEKLY_PLAN', 'DAILY_PLAN') 是否成立；成立时执行缩进的分支。
        if self.intent in ("WEEKLY_PLAN", "DAILY_PLAN"):
            # 检查条件 self.goal or self.stages or self.tasks or self.habits 是否成立；成立时执行缩进的分支。
            if self.goal or self.stages or self.tasks or self.habits:
                # 中止本轮正常处理，抛出 执行“调用 ValueError 完成输入值不合法的异常处理”得到的结果。
                raise ValueError("计划只能安排已有任务，不能复制成长对象")
            # 检查条件 not self.weeklyPlans or not self.taskAssignments 是否成立；成立时执行缩进的分支。
            if not self.weeklyPlans or not self.taskAssignments:
                # 中止本轮正常处理，抛出 执行“调用 ValueError 完成输入值不合法的异常处理”得到的结果。
                raise ValueError("计划必须包含周计划及已有任务安排")
            # 检查条件 self.intent == 'DAILY_PLAN' and (not self.dailyPlans) 是否成立；成立时执行缩进的分支。
            if self.intent == "DAILY_PLAN" and not self.dailyPlans:
                # 中止本轮正常处理，抛出 执行“调用 ValueError 完成输入值不合法的异常处理”得到的结果。
                raise ValueError("日计划必须包含安排日期")
        # 检查条件 self.intent == 'HABIT_PLAN' and (not self.habits or self.goal or self.stages or self.tasks or self.weeklyPlans or self.dailyPlans or self.taskAss… 是否成立；成立时执行缩进的分支。
        if self.intent == "HABIT_PLAN" and (
            # 继续传入或组合表达式 not self.habits 的结果，完成上面尚未结束的表达式。
            not self.habits
            # 继续补充本次调用或判断：表达式 or self.goal 的结果。
            or self.goal
            # 继续补充本次调用或判断：表达式 or self.stages 的结果。
            or self.stages
            # 继续补充本次调用或判断：表达式 or self.tasks 的结果。
            or self.tasks
            # 继续补充本次调用或判断：表达式 or self.weeklyPlans 的结果。
            or self.weeklyPlans
            # 继续补充本次调用或判断：表达式 or self.dailyPlans 的结果。
            or self.dailyPlans
            # 继续补充本次调用或判断：表达式 or self.taskAssignments 的结果。
            or self.taskAssignments
        ):
            # 中止本轮正常处理，抛出 执行“调用 ValueError 完成输入值不合法的异常处理”得到的结果。
            raise ValueError("习惯规划只包含独立习惯")
        # 把当前实例（self）交还给调用方，本函数到此结束。
        return self

    # 这个方法不依赖具体实例，可以直接通过类调用。
    @staticmethod
    # 函数 _validate_day：调用 _validate_day 完成检查合法性日处理；接收 周计划列表（weeks）、键（key）、值（value）。
    def _validate_day(weeks: dict[str, PlanWeekly], key: str, value: date) -> None:
        # 检查条件 key not in weeks or not 0 <= (value - weeks[key].weekStart).days < 7 是否成立；成立时执行缩进的分支。
        if key not in weeks or not 0 <= (value - weeks[key].weekStart).days < 7:
            # 中止本轮正常处理，抛出 执行“调用 ValueError 完成输入值不合法的异常处理”得到的结果。
            raise ValueError("日期不属于引用的自然周")
