/* 引入 ./http 提供的组件、状态函数或请求方法。 */
/* 封装成长助手工作区接口请求，供页面复用同一套路径、参数和响应处理。 */
import { api } from "./http";

export /* 查询条件（query）：调用 query 完成查询条件处理。 */ function query(values = {}) {
  const /* 参数集合（params）保存新建 地址搜索参数集合（URLSearchParams） 实例。 */ params = new URLSearchParams();
  /* 按这里的循环条件依次处理每个条目，直到条目读完或遇到退出条件。 */
  for (const /* 声明 键值（[key, value]），稍后再填入处理结果。 */ [key, value] of Object.entries(values)) {
    /* 检查value !== "" && value !== undefined && value !== null，即对应内容是否不存在，成立时走这一分支。 */
    if (value !== "" && value !== undefined && value !== null) /* 调用 params.set 完成设置处理。 */ params.set(key, value);
  }
  /* 返回根据前面的条件，从两个候选结果中选择一个：params.size ? `?${params}` : ""，当前函数到此结束。 */
  return params.size ? `?${params}` : "";
}

export const /* 工作区接口（workspaceApi）保存按表达式计算得到的值：{ get: (path, params, signal) => api(`/agent/${path}${query(params)}`, { signal }), post: (path, body) => api(`/agent/${…。 */ workspaceApi = {
  /* 读取（get）字段使用按表达式计算得到的值：(path, params, signal) => api(`/agent/${path}${query(params)}`, { signal })。 */
  get: (path, params, signal) => api(`/agent/${path}${query(params)}`, { /* 直接使用已有的取消信号（signal）作为同名字段。 */ signal }),
  /* post字段使用按表达式计算得到的值：(path, body) => api(`/agent/${path}`, { method: "POST", body })。 */
  post: (path, body) => api(`/agent/${path}`, { /* 方法（method）字段使用固定状态或编码 POST（POST）。 */ method: "POST", /* 直接使用已有的请求正文（body）作为同名字段。 */ body }),
  /* patch字段使用按表达式计算得到的值：(path, body) => api(`/agent/${path}`, { method: "PATCH", body })。 */
  patch: (path, body) => api(`/agent/${path}`, { /* 方法（method）字段使用固定状态或编码 PATCH（PATCH）。 */ method: "PATCH", /* 直接使用已有的请求正文（body）作为同名字段。 */ body }),
  /* 移除（remove）字段使用按表达式计算得到的值：(path, version) => api(`/agent/${path}${query({ version })}`, { method: "DELETE" })。 */
  remove: (path, version) => api(`/agent/${path}${query({ /* 直接使用已有的版本号（version）作为同名字段。 */ version })}`, { /* 方法（method）字段使用固定状态或编码 DELETE（删除）。 */ method: "DELETE" }),
  /* 上传（upload）字段使用根据前面的条件，从两个候选结果中选择一个：(file, { documentId, version, title, knowledgeBaseId, signal } = {}) => api( `/agent/knowledge${documentId ? `/${documentId}/…。 */
  // 单文件接口供批量队列复用，目标库在入队时固定，不随页面切库变化。
  upload: (file, { documentId, version, title, knowledgeBaseId, signal } = {}) => api(
    `/agent/knowledge${documentId ? `/${documentId}/content` : ""}${query({ fileName: file.name, version, title, knowledgeBaseId })}`,
    { method: documentId ? "PUT" : "POST", rawBody: file, signal,
      headers: { "Content-Type": "application/octet-stream", "X-File-Type": file.type || "application/octet-stream" } },
  ),
};

export const /* 时间文字（timeText）保存根据前面的条件，从两个候选结果中选择一个：(value) => value ? new Date(value).toLocaleString() : "—"。 */ timeText = (value) => value ? new Date(value).toLocaleString() : "—";
