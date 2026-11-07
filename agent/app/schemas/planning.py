"""规划快照契约；新建引用和既有对象引用明确区分，拒绝无法执行的空方案。"""

from datetime import date, time
from typing import Annotated
from zoneinfo import ZoneInfo, ZoneInfoNotFoundError

from pydantic import BaseModel, ConfigDict, Field, StringConstraints, model_validator

INTENTS = (
    "GOAL_PLAN",
    "STAGE_PLAN",
    "TASK_PLAN",
    "WEEKLY_PLAN",
    "DAILY_PLAN",
    "HABIT_PLAN",
    "COMPREHENSIVE_PLAN",
)
EntityId = Annotated[str, StringConstraints(pattern=r"^[1-9][0-9]{0,18}$")]


class PlanModel(BaseModel):
    model_config = ConfigDict(extra="forbid", str_strip_whitespace=True)


class PlanGoal(PlanModel):
    clientKey: str = Field(default="g1", min_length=1, max_length=64)
    title: str = Field(min_length=1, max_length=100)
    description: str | None = Field(default=None, max_length=2000)
    completionCriteria: str | None = Field(default=None, max_length=2000)
    startDate: date | None = None
    dueDate: date | None = None


class PlanStage(PlanGoal):
    clientKey: str = Field(min_length=1, max_length=64)
    goalKey: str | None = None
    goalId: EntityId | None = None
    goalVersion: int | None = Field(default=None, ge=0)
    sortOrder: int | None = Field(default=None, ge=0, le=9999)


class PlanTask(PlanGoal):
    clientKey: str = Field(min_length=1, max_length=64)
    stageKey: str | None = None
    stageId: EntityId | None = None
    stageVersion: int | None = Field(default=None, ge=0)
    priority: str | None = Field(default=None, pattern="^(LOW|MEDIUM|HIGH|URGENT)$")
    weight: int | None = Field(default=None, ge=1)
    estimatedMinutes: int | None = Field(default=None, ge=1, le=1440)
    sortOrder: int | None = Field(default=None, ge=0, le=9999)


class PlanWeekly(PlanModel):
    clientKey: str = Field(min_length=1, max_length=64)
    weekStart: date


class PlanDaily(PlanModel):
    clientKey: str = Field(min_length=1, max_length=64)
    weekKey: str
    businessDate: date


class PlanAssignment(PlanModel):
    clientKey: str = Field(min_length=1, max_length=64)
    taskKey: str | None = None
    taskId: EntityId | None = None
    taskVersion: int | None = Field(default=None, ge=0)
    assignmentId: EntityId | None = None
    assignmentVersion: int | None = Field(default=None, ge=0)
    weekKey: str
    businessDate: date | None = None
    sortOrder: int | None = Field(default=None, ge=0, le=9999)
    taskTitle: str | None = Field(default=None, max_length=100)
    previousWeekStart: date | None = None
    previousBusinessDate: date | None = None
    reason: str | None = Field(default=None, max_length=300)


class PlanHabit(PlanModel):
    clientKey: str = Field(min_length=1, max_length=64)
    name: str = Field(min_length=1, max_length=100)
    description: str | None = Field(default=None, max_length=2000)
    completionCriteria: str | None = Field(default=None, max_length=2000)
    categoryName: str | None = Field(default=None, max_length=100)
    cycleType: str = Field(default="DAILY", pattern="^(DAILY|WEEKLY_DAYS)$")
    weekdays: list[int] | None = Field(default=None, max_length=7)
    startDate: date | None = None
    expectedTime: time | None = None


class PlanSnapshot(PlanModel):
    intent: str
    timeZone: str | None = None
    goal: PlanGoal | None = None
    stages: list[PlanStage] = Field(default_factory=list, max_length=100)
    tasks: list[PlanTask] = Field(default_factory=list, max_length=500)
    weeklyPlans: list[PlanWeekly] = Field(default_factory=list, max_length=100)
    dailyPlans: list[PlanDaily] = Field(default_factory=list, max_length=700)
    taskAssignments: list[PlanAssignment] = Field(default_factory=list, max_length=500)
    habits: list[PlanHabit] = Field(default_factory=list, max_length=100)

    @model_validator(mode="after")
    def validate_relations(self) -> "PlanSnapshot":
        if self.intent not in INTENTS:
            raise ValueError("方案意图无效")
        if self.timeZone:
            try:
                ZoneInfo(self.timeZone)
            except (ZoneInfoNotFoundError, ValueError) as error:
                raise ValueError("账号时区无效") from error
        if not any(
            (
                self.goal,
                self.stages,
                self.tasks,
                self.weeklyPlans,
                self.dailyPlans,
                self.taskAssignments,
                self.habits,
            )
        ):
            raise ValueError("规划方案不能为空")
        keys: set[str] = set()
        entities = ([self.goal] if self.goal else []) + self.stages + self.tasks
        for entity in entities:
            if entity.startDate and entity.dueDate and entity.startDate > entity.dueDate:
                raise ValueError("开始日期不能晚于截止日期")
        for entity in (
            entities + self.weeklyPlans + self.dailyPlans + self.taskAssignments + self.habits
        ):
            if entity.clientKey in keys:
                raise ValueError("方案 clientKey 重复")
            keys.add(entity.clientKey)
        stage_keys = {stage.clientKey for stage in self.stages}
        task_keys = {task.clientKey for task in self.tasks}
        weeks = {week.clientKey: week for week in self.weeklyPlans}
        for stage in self.stages:
            new_parent = self.goal is not None and stage.goalId is None
            if new_parent:
                if stage.goalKey not in (None, self.goal.clientKey):
                    raise ValueError("阶段目标引用无效")
            elif stage.goalId is None or stage.goalVersion is None or stage.goalKey is not None:
                raise ValueError("已有目标引用必须携带 goalId 和 goalVersion")
        for task in self.tasks:
            if (task.stageKey is None) == (task.stageId is None):
                raise ValueError("任务必须且只能引用新阶段或已有阶段")
            if task.stageKey is not None and task.stageKey not in stage_keys:
                raise ValueError("任务阶段引用无效")
            if task.stageId is not None and task.stageVersion is None:
                raise ValueError("已有阶段引用必须携带 stageVersion")
        if len({week.weekStart for week in self.weeklyPlans}) != len(weeks):
            raise ValueError("同一周不能重复定义")
        if any(week.weekStart.weekday() != 0 for week in self.weeklyPlans):
            raise ValueError("weekStart 必须是自然周周一")
        for day in self.dailyPlans:
            self._validate_day(weeks, day.weekKey, day.businessDate)
        assigned: set[str] = set()
        for assignment in self.taskAssignments:
            if (assignment.taskKey is None) == (assignment.taskId is None):
                raise ValueError("安排必须且只能引用新任务或已有任务")
            if assignment.taskKey is not None and assignment.taskKey not in task_keys:
                raise ValueError("安排任务引用无效")
            if assignment.taskId is not None and assignment.taskVersion is None:
                raise ValueError("已有任务安排必须携带 taskVersion")
            if (assignment.assignmentId is None) != (assignment.assignmentVersion is None):
                raise ValueError("原安排标识与版本必须同时提供")
            identity = assignment.taskKey or "existing:" + str(assignment.taskId)
            if identity in assigned:
                raise ValueError("同一任务不能重复安排")
            assigned.add(identity)
            if assignment.weekKey not in weeks:
                raise ValueError("安排周计划引用无效")
            if assignment.businessDate:
                self._validate_day(weeks, assignment.weekKey, assignment.businessDate)
        for habit in self.habits:
            if habit.weekdays and (
                len(set(habit.weekdays)) != len(habit.weekdays)
                or any(type(day) is not int or day < 1 or day > 7 for day in habit.weekdays)
            ):
                raise ValueError("星期必须是互不重复的 1 至 7")
            if habit.cycleType == "WEEKLY_DAYS" and not habit.weekdays:
                raise ValueError("指定星期习惯必须提供 weekdays")
        if self.intent == "GOAL_PLAN" and self.goal is None:
            raise ValueError("目标规划必须包含目标")
        if self.intent == "STAGE_PLAN" and (self.goal is not None or not self.stages):
            raise ValueError("阶段规划应在已有目标下创建阶段")
        if self.intent == "TASK_PLAN" and (self.goal is not None or self.stages or not self.tasks):
            raise ValueError("任务规划应在已有阶段下创建任务")
        if self.intent in ("GOAL_PLAN", "STAGE_PLAN", "TASK_PLAN") and any(
            (self.weeklyPlans, self.dailyPlans, self.taskAssignments, self.habits)
        ):
            raise ValueError("当前规划意图不包含计划或习惯")
        if self.intent in ("WEEKLY_PLAN", "DAILY_PLAN"):
            if self.goal or self.stages or self.tasks or self.habits:
                raise ValueError("计划只能安排已有任务，不能复制成长对象")
            if not self.weeklyPlans or not self.taskAssignments:
                raise ValueError("计划必须包含周计划及已有任务安排")
            if self.intent == "DAILY_PLAN" and not self.dailyPlans:
                raise ValueError("日计划必须包含安排日期")
        if self.intent == "HABIT_PLAN" and (
            not self.habits
            or self.goal
            or self.stages
            or self.tasks
            or self.weeklyPlans
            or self.dailyPlans
            or self.taskAssignments
        ):
            raise ValueError("习惯规划只包含独立习惯")
        return self

    @staticmethod
    def _validate_day(weeks: dict[str, PlanWeekly], key: str, value: date) -> None:
        if key not in weeks or not 0 <= (value - weeks[key].weekStart).days < 7:
            raise ValueError("日期不属于引用的自然周")
