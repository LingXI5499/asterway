# 根据提交回执查询真实目标、任务、计划和习惯进度，不把提交成功当成任务完成。
# 方案创建后的真实业务进度；分开统计目标、任务、安排和今日习惯。

# 引入 Python 异步任务库（asyncio），供下面调用其功能。
import asyncio

# 从 app.core.errors 引入 带状态码和可读提示的 Agent 异常（AgentError）、Java后端接口错误（JavaApiError）。
from app.core.errors import AgentError, JavaApiError

# 从 app.services 引入 规划工作流业务服务（workflow_service）。
from app.services import workflow_service

# 把按字段名保存多个值的字典保存到 KINDS（_KINDS）。
_KINDS = {
    # 填写目标列表（goals）字段，值为表达式 ("goalIds", "goals", "goal:goal:read") 的结果。
    "goals": ("goalIds", "goals", "goal:goal:read"),
    # 填写阶段列表（stages）字段，值为表达式 ("stageIds", "stages", "stage:stage:read") 的结果。
    "stages": ("stageIds", "stages", "stage:stage:read"),
    # 填写任务列表（tasks）字段，值为表达式 ("taskIds", "tasks", "task:task:read") 的结果。
    "tasks": ("taskIds", "tasks", "task:task:read"),
    # 填写周计划列表（weeks）字段，值为表达式 ("weeklyPlanIds", "weekly-plans", "planning:weekly:read") 的结果。
    "weeks": ("weeklyPlanIds", "weekly-plans", "planning:weekly:read"),
    # 填写日列表（days）字段，值为表达式 ("dailyPlanIds", "daily-plans", "planning:daily:read") 的结果。
    "days": ("dailyPlanIds", "daily-plans", "planning:daily:read"),
    # 填写习惯列表（habits）字段，值为表达式 ("habitIds", "habits", "habit:habit:read") 的结果。
    "habits": ("habitIds", "habits", "habit:habit:read"),
}


# 异步函数 collect：调用 collect 完成collect处理；接收 Agent 数据库访问器（db）、上下文（context）、客户端（client）、方案运行编号（run_id）。
async def collect(db, context, client, run_id):
    # 调用 context.require 完成require处理。
    context.require("agent:workflow:read")
    # 把等待执行“调用 workflow_service.detail 完成详情处理”得到的结果完成后的结果保存到 详情（detail）。
    detail = await workflow_service.detail(db, user_id=int(context.user_id), run_id=run_id)
    # 把执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果或者按字段名保存多个值的字典，这里先创建空字典保存到 提交回执（receipt）。
    receipt = detail.get("commit") or {}
    # 检查条件 receipt.get('status') != 'SUCCEEDED' 是否成立；成立时执行缩进的分支。
    if receipt.get("status") != "SUCCEEDED":
        # 把按字段名保存多个值的字典交还给调用方，本函数到此结束。
        return {"ready": False, "groups": {}, "signals": [], "partial": False}
    # 把执行“调用 next 完成下一个处理”得到的结果保存到 方案（plan）。
    plan = next((item for item in detail["plans"] if item["id"] == detail["run"]["latestPlanVersionId"]), {})
    # 把执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果或者按字段名保存多个值的字典，这里先创建空字典保存到 确认时的数据快照（snapshot）。
    snapshot = plan.get("snapshot") or {}
    # 把执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果或者按字段名保存多个值的字典，这里先创建空字典保存到 结果（result）。
    result = receipt.get("result") or {}
    # 把逐项处理数据，并按这里的条件筛选或转换后收集结果保存到 编号列表（ids）。
    ids = {kind: list(dict.fromkeys(str(value) for value in result.get(field, [])))
           # 继续传入或组合调用 for kind, (field, _, _) in _KINDS.items 完成数据列表处理，完成上面尚未结束的表达式。
           for kind, (field, _, _) in _KINDS.items()}
    # 从依次包含 依次包含 固定文字 'stages'、固定文字 'goalId'、固定文字 'goals' 的集合、依次包含 固定文字 'tasks'、固定文字 'stageId'、固定文字 'stages' 的集合、依次包含 固定文字 'taskAssignments'、固定文字 'taskId'、固定文字 'tasks' 的集合 的集合依次取出来源字段种类（(source, field, kind)），每项执行一次下面的处理。
    for source, field, kind in (("stages", "goalId", "goals"), ("tasks", "stageId", "stages"),
                                # 继续传入或组合表达式 ("taskAssignments", "taskId", "tasks")): 的结果，完成上面尚未结束的表达式。
                                ("taskAssignments", "taskId", "tasks")):
        # 把执行“调用 list 完成列表处理”得到的结果保存到 编号列表种类（ids[kind]）。
        ids[kind] = list(dict.fromkeys([*ids[kind], *(str(row[field]) for row in snapshot.get(source, []) if row.get(field))]))
    # 把等待执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果完成后的结果保存到 calendar。
    calendar = await client.get("dashboard/overview", authorization=context.authorization, trace_id=context.trace_id)
    # 把条件 isinstance(calendar.data, dict) 是否成立时使用执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果，否则使用空值，表示当前还没有对应内容保存到 今天（today）。
    today = calendar.data.get("today") if isinstance(calendar.data, dict) else None
    # 检查条件 not today 是否成立；成立时执行缩进的分支。
    if not today:
        # 中止本轮正常处理，抛出 执行“构造带 HTTP 状态码、业务错误码和可读提示的异常”得到的结果。
        raise AgentError(502, "ACCOUNT_CALENDAR_UNAVAILABLE", "暂时无法取得账号业务日期")
    # 把执行“调用 asyncio.Semaphore 完成并发名额控制器处理”得到的结果保存到 slots。
    slots = asyncio.Semaphore(4)
    # 把空列表或集合保存到 错误处理器（errors）。
    errors = []
    # 把执行“调用 any 完成any处理”得到的结果保存到 是否只有部分结果（partial）。
    partial = any(len(values) > 20 for values in ids.values())

    # 异步函数 get：按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值；接收 路径（path）、参数集合（params）。
    async def get(path, params=None):
        # 执行可能失败的处理；异常进入 except，finally 负责一定要执行的收尾。
        try:
            # 异步进入slots；退出时由上下文管理器提交、回滚或释放资源。
            async with slots:
                # 把等待执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果完成后的结果保存到 值（value）。
                value = await client.get(path, authorization=context.authorization,
                                          # 传入追踪标识编号（trace_id），值为表达式 context.trace_id, params=params) 的结果。
                                          trace_id=context.trace_id, params=params)
            # 把值（value）的数据（data）属性交还给调用方，本函数到此结束。
            return value.data
        # 捕获JavaApiError异常，执行下面的错误处理，异常对象保存为 error。
        except JavaApiError as error:
            # 检查条件 error.status_code == 401 是否成立；成立时执行缩进的分支。
            if error.status_code == 401:
                # 中止本轮正常处理，把当前异常继续向上交给调用方。
                raise
            # 把新条目放到列表末尾。
            errors.append("部分业务进度暂未读取成功，请刷新重试")
            # 把空值，表示当前还没有对应内容交还给调用方，本函数到此结束。
            return None

    # 异步函数 row：调用 row 完成当前记录处理；接收 种类（kind）、编号（entity_id）。
    async def row(kind, entity_id):
        # 把从KINDS（_KINDS）中按种类（kind）取出的内容保存到 路径（(_, path, _)）。
        _, path, _ = _KINDS[kind]
        # 把等待执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果完成后的结果保存到 值（value）。
        value = await get(f"{path}/{entity_id}")
        # 检查条件 not isinstance(value, dict) 是否成立；成立时执行缩进的分支。
        if not isinstance(value, dict):
            # 把空值，表示当前还没有对应内容交还给调用方，本函数到此结束。
            return None
        # 把依次包含 固定文字 'id'、固定文字 'name'、固定文字 'title'、固定文字 'status'、固定文字 'progress'等内容 的集合保存到 fields。
        fields = ("id", "name", "title", "status", "progress", "goalId", "stageId", "dueDate", "expectedEndDate",
                  # 继续传入或组合表达式 "weekStartDate", "weekEndDate", "planDate", "checkedToday") 的结果，完成上面尚未结束的表达式。
                  "weekStartDate", "weekEndDate", "planDate", "checkedToday")
        # 把逐项处理数据，并按这里的条件筛选或转换后收集结果保存到 单条数据（item）。
        item = {key: value[key] for key in fields if value.get(key) is not None}
        # 把执行“调用 str 完成str处理”得到的结果保存到 单条数据编号（item['id']）。
        item["id"] = str(value.get("id", entity_id))
        # 把种类（kind）保存到 单条数据种类（item['kind']）。
        item["kind"] = kind
        # 检查条件 kind in {'weeks', 'days'} 是否成立；成立时执行缩进的分支。
        if kind in {"weeks", "days"}:
            # 把等待执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果完成后的结果保存到 数据（data）。
            data = await get(f"{path}/{entity_id}/tasks", {"page": 1, "size": 100})
            # 检查条件 isinstance(data, dict) and isinstance(data.get('items'), list) 是否成立；成立时执行缩进的分支。
            if isinstance(data, dict) and isinstance(data.get("items"), list):
                # 把从数据（data）中按固定文字 'items'取出的内容保存到 数据列表（items）。
                items = data["items"]
                # 把执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果保存到 总数（total）。
                total = data.get("total", len(items))
                # 把总数（total）保存到 单条数据任务总数（item['taskTotal']）。
                item["taskTotal"] = total
                # 把执行“调用 len 完成len处理”得到的结果保存到 单条数据任务已返回（item['taskReturned']）。
                item["taskReturned"] = len(items)
                # 把执行“调用 sum 完成sum处理”得到的结果保存到 单条数据任务已完成（item['taskCompleted']）。
                item["taskCompleted"] = sum(task.get("status") == "COMPLETED" for task in items)
                # 把条件 total > len(items) 是否成立保存到 单条数据部分结果（item['partial']）。
                item["partial"] = total > len(items)
                # 把执行“调用 sum 完成sum处理”得到的结果保存到 单条数据已逾期（item['overdue']）。
                item["overdue"] = sum(task.get("status") != "COMPLETED" and
                    # 继续传入或组合优先使用前面的有效值，没有时采用后面的备用值：(value.get("planDate") or value.get("weekEndDate") or today) < today for task in items)，完成上面尚未结束的表达式。
                    (value.get("planDate") or value.get("weekEndDate") or today) < today for task in items)
        # 检查条件 kind == 'tasks' 是否成立；成立时执行缩进的分支。
        if kind == "tasks":
            # 把执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果或者执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果保存到 应完成（due）。
            due = value.get("dueDate") or value.get("expectedEndDate")
            # 把执行“调用 bool 完成bool处理”得到的结果保存到 单条数据已逾期（item['overdue']）。
            item["overdue"] = bool(due and due < today and value.get("status") != "COMPLETED")
        # 把单条数据（item）交还给调用方，本函数到此结束。
        return item

    # 异步函数 group：调用 group 完成分组处理；接收 种类（kind）。
    async def group(kind):
        # 检查当前用户是否具有需要的权限：_KINDS[kind][2] not in context.permissions；成立时执行缩进的分支。
        if _KINDS[kind][2] not in context.permissions:
            # 检查条件 ids[kind] 是否成立；成立时执行缩进的分支。
            if ids[kind]:
                # 把新条目放到列表末尾。
                errors.append("部分对象没有读取权限")
            # 把空列表或集合交还给调用方，本函数到此结束。
            return []
        # 把逐项处理数据，并按这里的条件筛选或转换后收集结果交还给调用方，本函数到此结束。
        return [value for value in await asyncio.gather(*(row(kind, value) for value in ids[kind][:20])) if value]

    # 把执行“调用 dict 完成dict处理”得到的结果保存到 分组列表（groups）。
    groups = dict(zip(_KINDS, await asyncio.gather(*(group(kind) for kind in _KINDS)), strict=True))
    # 既有阶段/任务方案也能跟踪所属目标，不限于本次新建的 goalIds。
    # 从依次包含 依次包含 固定文字 'tasks'、固定文字 'stageId'、固定文字 'stages' 的集合、依次包含 固定文字 'stages'、固定文字 'goalId'、固定文字 'goals' 的集合 的集合依次取出种类上级字段上级种类（(kind, parent_field, parent_kind)），每项执行一次下面的处理。
    for kind, parent_field, parent_kind in (("tasks", "stageId", "stages"), ("stages", "goalId", "goals")):
        # 把逐项处理数据，并按这里的条件筛选或转换后收集结果保存到 known。
        known = {item["id"] for item in groups[parent_kind]}
        # 把从执行“调用 list 完成列表处理”得到的结果中按表达式 :20 的结果取出的内容保存到 parents。
        parents = list(dict.fromkeys(str(item[parent_field]) for item in groups[kind]
                                    # 继续传入或组合按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值，完成上面尚未结束的表达式。
                                    if item.get(parent_field) and str(item[parent_field]) not in known))[:20]
        # 检查当前用户是否具有需要的权限：_KINDS[parent_kind][2] in context.permissions；成立时执行缩进的分支。
        if _KINDS[parent_kind][2] in context.permissions:
            # 把另一组条目依次加入列表。
            groups[parent_kind].extend(value for value in await asyncio.gather(*(row(parent_kind, value) for value in parents)) if value)
    # 检查当前用户是否具有需要的权限：groups['habits'] and 'habit:habit:list' in context.permissions；成立时执行缩进的分支。
    if groups["habits"] and "habit:habit:list" in context.permissions:
        # 把等待执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果完成后的结果保存到 数据（data）。
        data = await get("habits/today", {"page": 1, "size": 100})
        # 检查条件 isinstance(data, dict) and isinstance(data.get('items'), list) 是否成立；成立时执行缩进的分支。
        if isinstance(data, dict) and isinstance(data.get("items"), list):
            # 把逐项处理数据，并按这里的条件筛选或转换后收集结果保存到 应完成（due）。
            due = {str(item.get("id") or item.get("habitId")): item for item in data["items"]}
            # 把条件 data.get('total', len(due)) <= len(data['items']) 是否成立保存到 完成列表（complete_list）。
            complete_list = data.get("total", len(due)) <= len(data["items"])
            # 从从分组列表（groups）中按固定文字 'habits'取出的内容依次取出单条数据（item），每项执行一次下面的处理。
            for item in groups["habits"]:
                # 把执行“按指定键或参数读取对象中的内容；未找到时使用调用中给出的备用值”得到的结果保存到 实际（actual）。
                actual = due.get(item["id"])
                # 把条件 actual is not None 是否成立或者完成列表（complete_list）保存到 单条数据今天Known（item['todayKnown']）。
                item["todayKnown"] = actual is not None or complete_list
                # 把条件 actual is not None 是否成立保存到 单条数据应完成今天（item['dueToday']）。
                item["dueToday"] = actual is not None
                # 把执行“调用 bool 完成bool处理”得到的结果保存到 单条数据已核对今天（item['checkedToday']）。
                item["checkedToday"] = bool(actual and (actual.get("checkinId") or actual.get("checkedToday")
                                                         # 继续补充本次调用或判断：优先使用前面的有效值，没有时采用后面的备用值：or (actual.get("doneCount") or 0) > 0))。
                                                         or (actual.get("doneCount") or 0) > 0))
    # 把执行“调用 sum 完成sum处理”得到的结果保存到 已逾期（overdue）。
    overdue = sum(bool(item.get("overdue")) for item in groups["tasks"])
    # 把执行“调用 sum 完成sum处理”得到的结果保存到 方案已逾期（plan_overdue）。
    plan_overdue = sum(item.get("overdue", 0) for kind in ("weeks", "days") for item in groups[kind])
    # 把执行“调用 sum 完成sum处理”得到的结果保存到 已完成（completed）。
    completed = sum(item.get("status") == "COMPLETED" for item in groups["tasks"])
    # 把空列表或集合保存到 提示列表（signals）。
    signals = []
    # 检查条件 overdue or plan_overdue 是否成立；成立时执行缩进的分支。
    if overdue or plan_overdue:
        # 把新条目放到列表末尾。
        signals.append({"code": "OVERDUE", "message": "有任务超过截止或原安排时间，可以复盘后调整后续安排"})
    # 检查条件 completed 是否成立；成立时执行缩进的分支。
    if completed:
        # 把新条目放到列表末尾。
        signals.append({"code": "PROGRESS", "message": f"已完成 {completed} 个跟踪任务，可以根据实际投入安排下一步"})
    # 把按字段名保存多个值的字典交还给调用方，本函数到此结束。
    return {"ready": True, "asOf": today, "groups": groups, "signals": signals,
            # 填写是否只有部分结果（partial）字段，值为优先使用前面的有效值，没有时采用后面的备用值：partial or bool(errors) or any(item.get("partial") for values in groups.values() for item in values)。
            "partial": partial or bool(errors) or any(item.get("partial") for values in groups.values() for item in values),
            # 填写说明列表（notes）字段，值为调用 list(dict.fromkeys 完成fromkeys处理。
            "notes": list(dict.fromkeys(errors)), "trackedCounts": {kind: len(values) for kind, values in groups.items()}}
