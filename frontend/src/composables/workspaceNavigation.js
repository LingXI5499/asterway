/* 组合工作区Navigation相关的页面状态、数据读取和清理逻辑。 */
export /* 工作区页数（workspacePages）：调用 workspacePages 完成工作区页数处理。 */ function workspacePages(nodes) {
  const /* 结果（result）保存一个空列表，用来保存后续多条数据。 */ result = [];
  const /* 已读（seen）保存新建 设置（Set） 实例。 */ seen = new Set();
  /* visit：调用 visit 完成visit处理。 */
  function visit(items) {
    /* 按这里的循环条件依次处理每个条目，直到条目读完或遇到退出条件。 */
    for (const /* 声明 单条数据（item），稍后再填入处理结果。 */ item of items) {
      /* 检查条件 item.menuType === "PAGE" && item.routePath && !seen.has(item.routePath) 是否成立，成立时走这一分支。 */
      if (item.menuType === "PAGE" && item.routePath && !seen.has(item.routePath)) {
        /* 调用 result.push 完成push处理。 */
        result.push(item);
        /* 加入集合或登记一条待保存记录。 */
        seen.add(item.routePath);
      }
      /* 调用 visit 完成visit处理。 */
      visit(item.children || []);
    }
  }
  /* 调用 visit 完成visit处理。 */
  visit(nodes || []);
  /* 返回结果（result），当前函数到此结束。 */
  return result;
}

export /* 是否规划路径（isPlanningPath）：判断是否为 规划路径（PlanningPath）。 */ function isPlanningPath(path) {
  /* 返回优先使用前面的有效值，没有时采用后面的备用值：path.startsWith("/growth/") || path.startsWith("/planning/")，当前函数到此结束。 */
  return path.startsWith("/growth/") || path.startsWith("/planning/");
}

// 周、日和任务入口在导航中合并为“计划”，原有地址仍由路由层兼容。
export function planningNavigation(pages) {
  const result = [];
  let added = false;
  for (const item of pages.filter(item => isPlanningPath(item.routePath))) {
    if (item.routePath.startsWith("/planning/") || item.routePath === "/growth/tasks") {
      if (!added) result.push({ ...item, name: "计划", routePath: "/planning/plans" });
      added = true;
    } else result.push(item);
  }
  return result;
}
