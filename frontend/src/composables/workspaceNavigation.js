export function workspacePages(nodes) {
  const result = [];
  const seen = new Set();
  function visit(items) {
    for (const item of items) {
      if (item.menuType === "PAGE" && item.routePath && !seen.has(item.routePath)) {
        result.push(item);
        seen.add(item.routePath);
      }
      visit(item.children || []);
    }
  }
  visit(nodes || []);
  return result;
}

export function isPlanningPath(path) {
  return path.startsWith("/growth/") || path.startsWith("/planning/");
}
