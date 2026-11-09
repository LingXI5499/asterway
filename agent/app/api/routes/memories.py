# 提供长期记忆列表内部 HTTP 路由，校验当前用户后调用对应服务。
# 长期记忆内部接口；浏览器只通过 Java 代理访问，用户范围由 Java 验证后的上下文决定。

# 从 typing 引入 给类型附加校验或依赖说明的类型标记（Annotated）、只能从列出的固定值中选择的类型（Literal）。
from typing import Annotated, Literal

# 从 fastapi 引入 同一组 HTTP 路由的登记器（APIRouter）、由 FastAPI 提供参数和前置校验的依赖声明（Depends）、查询条件（Query）。
from fastapi import APIRouter, Depends, Query

# 从 app.core.context 引入 成长助手上下文（AgentContext）、读取成长助手上下文（get_agent_context）。
from app.core.context import AgentContext, get_agent_context

# 从 app.db.session 引入 数据库（Database）、取得本次请求的 Agent 数据库访问器（get_database）。
from app.db.session import Database, get_database

# 从 app.schemas.memory 引入 长期记忆候选请求（MemoryCandidateRequest）、长期记忆当前页（MemoryPageVO）、长期记忆更新请求（MemoryUpdateRequest）、长期记忆版本号请求（MemoryVersionRequest）、长期记忆（MemoryVO）。
from app.schemas.memory import (
    # 继续传入或组合长期记忆候选请求（MemoryCandidateRequest），完成上面尚未结束的表达式。
    MemoryCandidateRequest,
    # 继续传入或组合长期记忆当前页（MemoryPageVO），完成上面尚未结束的表达式。
    MemoryPageVO,
    # 继续传入或组合长期记忆更新请求（MemoryUpdateRequest），完成上面尚未结束的表达式。
    MemoryUpdateRequest,
    # 继续传入或组合长期记忆版本号请求（MemoryVersionRequest），完成上面尚未结束的表达式。
    MemoryVersionRequest,
    # 继续传入或组合长期记忆（MemoryVO），完成上面尚未结束的表达式。
    MemoryVO,
)

# 从 app.services 引入 长期记忆业务服务（memory_service）。
from app.services import memory_service

# 把执行“调用 APIRouter 完成同一组 HTTP 路由的登记器处理”得到的结果保存到 HTTP 路由登记器（router）。
router = APIRouter(prefix="/internal/agent/memories", tags=["Memories"])

# 把从给类型附加校验或依赖说明的类型标记（Annotated）中按依次包含 成长助手上下文（AgentContext）、执行“调用 Depends 完成由 FastAPI 提供参数和前置校验的依赖声明处理”得到的结果 的集合取出的内容保存到 上下文Dep（ContextDep）。
ContextDep = Annotated[AgentContext, Depends(get_agent_context)]
# 把从给类型附加校验或依赖说明的类型标记（Annotated）中按依次包含 数据库（Database）、执行“调用 Depends 完成由 FastAPI 提供参数和前置校验的依赖声明处理”得到的结果 的集合取出的内容保存到 数据库Dep（DbDep）。
DbDep = Annotated[Database, Depends(get_database)]


# 函数 _user：调用 _user 完成用户处理；接收 上下文（context）。
def _user(context: AgentContext) -> int:
    # 把执行“调用 int 完成int处理”得到的结果交还给调用方，本函数到此结束。
    return int(context.user_id)


# 把下面的函数登记为 HTTP 处理入口：router.get("", response_model=MemoryPageVO, summary="记忆列表")。
@router.get("", response_model=MemoryPageVO, summary="记忆列表")
# 异步函数 page：调用 page 完成当前页处理；接收 上下文（context）、Agent 数据库访问器（db）、状态（status）、种类（kind）、keyword、当前页数值（page_number）、每页条数（size）。
async def page(
    # 填写上下文（context）字段，值为上下文Dep（ContextDep）。
    context: ContextDep,
    # 填写Agent 数据库访问器（db）字段，值为数据库Dep（DbDep）。
    db: DbDep,
    # 填写状态（status）字段，值为表达式 Annotated[Literal["CANDIDATE", "CONFIRMED", "REJECTED", "ARCHIVED"] | None, Query()] = None 的结果。
    status: Annotated[Literal["CANDIDATE", "CONFIRMED", "REJECTED", "ARCHIVED"] | None, Query()] = None,
    # 填写种类（kind）字段，值为表达式 Annotated[Literal["PREFERENCE", "FACT", "CONSTRAINT", "GOAL_CONTEXT"] | None, Query()] = None 的结果。
    kind: Annotated[Literal["PREFERENCE", "FACT", "CONSTRAINT", "GOAL_CONTEXT"] | None, Query()] = None,
    # 填写keyword字段，值为表达式 Annotated[str | None, Query(max_length=100)] = None 的结果。
    keyword: Annotated[str | None, Query(max_length=100)] = None,
    # 填写当前页数值（page_number）字段，值为表达式 Annotated[int, Query(alias="page", ge=1)] = 1 的结果。
    page_number: Annotated[int, Query(alias="page", ge=1)] = 1,
    # 填写每页条数（size）字段，值为表达式 Annotated[int, Query(ge=1, le=100)] = 20 的结果。
    size: Annotated[int, Query(ge=1, le=100)] = 20,
# 继续传入或组合表达式 ) -> MemoryPageVO: 的结果，完成上面尚未结束的表达式。
) -> MemoryPageVO:
    # 调用 context.require 完成require处理。
    context.require("agent:memory:read")
    # 把等待执行“列出 长期记忆列表（memories）”得到的结果完成后的结果保存到 记录列表总数（(rows, total)）。
    rows, total = await memory_service.list_memories(
        # 继续传入或组合Agent 数据库访问器（db），完成上面尚未结束的表达式。
        db,
        # 继续传入或组合调用 _user 完成用户处理，完成上面尚未结束的表达式。
        _user(context),
        # 传入状态（status），值为状态（status）。
        status=status,
        # 传入种类（kind），值为种类（kind）。
        kind=kind,
        # 传入keyword，值为keyword。
        keyword=keyword,
        # 传入当前页（page），值为当前页数值（page_number）。
        page=page_number,
        # 传入每页条数（size），值为每页条数（size）。
        size=size,
    )
    # 把执行“调用 MemoryPageVO 完成长期记忆当前页处理”得到的结果交还给调用方，本函数到此结束。
    return MemoryPageVO(
        # 传入数据列表（items），值为调用 [memory_service.memory_vo 完成长期记忆处理。
        items=[memory_service.memory_vo(row) for row in rows],
        # 传入总数（total），值为总数（total）。
        total=total,
        # 传入当前页（page），值为当前页数值（page_number）。
        page=page_number,
        # 传入每页条数（size），值为每页条数（size）。
        size=size,
    )


# 把下面的函数登记为 HTTP 处理入口：router.post("/candidates", status_code=201, response_model=MemoryVO, summary="新建记忆候选")。
@router.post("/candidates", status_code=201, response_model=MemoryVO, summary="新建记忆候选")
# 异步函数 create_candidate：创建 候选（candidate）；接收 提交数据（payload）、上下文（context）、Agent 数据库访问器（db）。
async def create_candidate(
    # 填写提交数据（payload）字段，值为表达式 MemoryCandidateRequest, context: ContextDep, db: DbDep 的结果。
    payload: MemoryCandidateRequest, context: ContextDep, db: DbDep
# 继续传入或组合表达式 ) -> MemoryVO: 的结果，完成上面尚未结束的表达式。
) -> MemoryVO:
    # 调用 context.require 完成require处理。
    context.require("agent:memory:manage")
    # 把等待执行“创建 候选（candidate）”得到的结果完成后的结果保存到 当前记录（row）。
    row = await memory_service.create_candidate(db, _user(context), payload)
    # 把执行“调用 memory_service.memory_vo 完成长期记忆处理”得到的结果交还给调用方，本函数到此结束。
    return memory_service.memory_vo(row)


# 把下面的函数登记为 HTTP 处理入口：router.patch("/{memory_id}", response_model=MemoryVO, summary="修改记忆内容")。
@router.patch("/{memory_id}", response_model=MemoryVO, summary="修改记忆内容")
# 异步函数 update_content：更新 内容（content）；接收 长期记忆编号（memory_id）、提交数据（payload）、上下文（context）、Agent 数据库访问器（db）。
async def update_content(
    # 填写长期记忆编号（memory_id）字段，值为int。
    memory_id: int,
    # 填写提交数据（payload）字段，值为长期记忆更新请求（MemoryUpdateRequest）。
    payload: MemoryUpdateRequest,
    # 填写上下文（context）字段，值为上下文Dep（ContextDep）。
    context: ContextDep,
    # 填写Agent 数据库访问器（db）字段，值为数据库Dep（DbDep）。
    db: DbDep,
# 继续传入或组合表达式 ) -> MemoryVO: 的结果，完成上面尚未结束的表达式。
) -> MemoryVO:
    # 调用 context.require 完成require处理。
    context.require("agent:memory:manage")
    # 把等待执行“更新 内容（content）”得到的结果完成后的结果保存到 当前记录（row）。
    row = await memory_service.update_content(
        # 继续传入或组合表达式 db, _user(context), memory_id, payload.content, payload.version 的结果，完成上面尚未结束的表达式。
        db, _user(context), memory_id, payload.content, payload.version
    )
    # 把执行“调用 memory_service.memory_vo 完成长期记忆处理”得到的结果交还给调用方，本函数到此结束。
    return memory_service.memory_vo(row)


# 把下面的函数登记为 HTTP 处理入口：router.post("/{memory_id}/confirm", response_model=MemoryVO, summary="确认记忆")。
@router.post("/{memory_id}/confirm", response_model=MemoryVO, summary="确认记忆")
# 异步函数 confirm：调用 confirm 完成确认处理；接收 长期记忆编号（memory_id）、提交数据（payload）、上下文（context）、Agent 数据库访问器（db）。
async def confirm(
    # 填写长期记忆编号（memory_id）字段，值为int。
    memory_id: int,
    # 填写提交数据（payload）字段，值为长期记忆版本号请求（MemoryVersionRequest）。
    payload: MemoryVersionRequest,
    # 填写上下文（context）字段，值为上下文Dep（ContextDep）。
    context: ContextDep,
    # 填写Agent 数据库访问器（db）字段，值为数据库Dep（DbDep）。
    db: DbDep,
# 继续传入或组合表达式 ) -> MemoryVO: 的结果，完成上面尚未结束的表达式。
) -> MemoryVO:
    # 调用 context.require 完成require处理。
    context.require("agent:memory:manage")
    # 把等待执行“调用 memory_service.confirm 完成确认处理”得到的结果完成后的结果保存到 当前记录（row）。
    row = await memory_service.confirm(db, _user(context), memory_id, payload.version)
    # 把执行“调用 memory_service.memory_vo 完成长期记忆处理”得到的结果交还给调用方，本函数到此结束。
    return memory_service.memory_vo(row)


# 把下面的函数登记为 HTTP 处理入口：router.post("/{memory_id}/reject", response_model=MemoryVO, summary="拒绝记忆")。
@router.post("/{memory_id}/reject", response_model=MemoryVO, summary="拒绝记忆")
# 异步函数 reject：调用 reject 完成拒绝处理；接收 长期记忆编号（memory_id）、提交数据（payload）、上下文（context）、Agent 数据库访问器（db）。
async def reject(
    # 填写长期记忆编号（memory_id）字段，值为int。
    memory_id: int,
    # 填写提交数据（payload）字段，值为长期记忆版本号请求（MemoryVersionRequest）。
    payload: MemoryVersionRequest,
    # 填写上下文（context）字段，值为上下文Dep（ContextDep）。
    context: ContextDep,
    # 填写Agent 数据库访问器（db）字段，值为数据库Dep（DbDep）。
    db: DbDep,
# 继续传入或组合表达式 ) -> MemoryVO: 的结果，完成上面尚未结束的表达式。
) -> MemoryVO:
    # 调用 context.require 完成require处理。
    context.require("agent:memory:manage")
    # 把等待执行“调用 memory_service.reject 完成拒绝处理”得到的结果完成后的结果保存到 当前记录（row）。
    row = await memory_service.reject(db, _user(context), memory_id, payload.version)
    # 把执行“调用 memory_service.memory_vo 完成长期记忆处理”得到的结果交还给调用方，本函数到此结束。
    return memory_service.memory_vo(row)


# 把下面的函数登记为 HTTP 处理入口：router.post("/{memory_id}/archive", response_model=MemoryVO, summary="归档记忆")。
@router.post("/{memory_id}/archive", response_model=MemoryVO, summary="归档记忆")
# 异步函数 archive：调用 archive 完成归档处理；接收 长期记忆编号（memory_id）、提交数据（payload）、上下文（context）、Agent 数据库访问器（db）。
async def archive(
    # 填写长期记忆编号（memory_id）字段，值为int。
    memory_id: int,
    # 填写提交数据（payload）字段，值为长期记忆版本号请求（MemoryVersionRequest）。
    payload: MemoryVersionRequest,
    # 填写上下文（context）字段，值为上下文Dep（ContextDep）。
    context: ContextDep,
    # 填写Agent 数据库访问器（db）字段，值为数据库Dep（DbDep）。
    db: DbDep,
# 继续传入或组合表达式 ) -> MemoryVO: 的结果，完成上面尚未结束的表达式。
) -> MemoryVO:
    # 调用 context.require 完成require处理。
    context.require("agent:memory:manage")
    # 把等待执行“调用 memory_service.archive 完成归档处理”得到的结果完成后的结果保存到 当前记录（row）。
    row = await memory_service.archive(db, _user(context), memory_id, payload.version)
    # 把执行“调用 memory_service.memory_vo 完成长期记忆处理”得到的结果交还给调用方，本函数到此结束。
    return memory_service.memory_vo(row)


# 把下面的函数登记为 HTTP 处理入口：router.delete("/{memory_id}", response_model=MemoryVO, summary="删除记忆")。
@router.delete("/{memory_id}", response_model=MemoryVO, summary="删除记忆")
# 异步函数 remove：调用 remove 完成移除处理；接收 长期记忆编号（memory_id）、版本号（version）、上下文（context）、Agent 数据库访问器（db）。
async def remove(
    # 填写长期记忆编号（memory_id）字段，值为int。
    memory_id: int,
    # 填写版本号（version）字段，值为表达式 Annotated[int, Query(ge=0)] 的结果。
    version: Annotated[int, Query(ge=0)],
    # 填写上下文（context）字段，值为上下文Dep（ContextDep）。
    context: ContextDep,
    # 填写Agent 数据库访问器（db）字段，值为数据库Dep（DbDep）。
    db: DbDep,
# 继续传入或组合表达式 ) -> MemoryVO: 的结果，完成上面尚未结束的表达式。
) -> MemoryVO:
    # 调用 context.require 完成require处理。
    context.require("agent:memory:manage")
    # 把等待执行“调用 memory_service.delete 完成删除处理”得到的结果完成后的结果保存到 当前记录（row）。
    row = await memory_service.delete(db, _user(context), memory_id, version)
    # 把执行“调用 memory_service.memory_vo 完成长期记忆处理”得到的结果交还给调用方，本函数到此结束。
    return memory_service.memory_vo(row)
