/* 引入 element-plus 提供的组件、状态函数或请求方法。 */
/* 封装页面接口请求，供页面复用同一套路径、参数和响应处理。 */
import { ElMessage, ElMessageBox } from "element-plus";
export const /* 复盘报告（report）保存优先使用前面的有效值，没有时采用后面的备用值：(error) => { if (!["cancel", "close"].includes(error)) ElMessage.error(error.message || "操作失败"); }。 */ report = (error) => {
  /* 检查条件 !["cancel", "close"].includes(error) 是否成立，成立时走这一分支。 */
  if (!["cancel", "close"].includes(error))
    /* 调用 ElMessage.error 完成错误处理。 */
    ElMessage.error(error.message || "操作失败");
};
export const /* 确认操作（confirmAction）保存调用 (message, confirmButtonText = "确认操作") => ElMessageBox.confirm 完成确认处理。 */ confirmAction = (message, confirmButtonText = "确认操作") =>
  ElMessageBox.confirm(message, "确认操作", {
    /* 类型（type）字段使用固定文字 'warning'。 */
    type: "warning",
    /* 直接使用已有的确认Button文字（confirmButtonText）作为同名字段。 */
    confirmButtonText,
    /* 取消Button文字（cancelButtonText）字段使用固定文字 '取消'。 */
    cancelButtonText: "取消",
  });
export const /* 日期文字（dateText）保存根据前面的条件，从两个候选结果中选择一个：(value) => value ? new Date(value).toLocaleString("zh-CN") : "—"。 */ dateText = (value) =>
  value ? new Date(value).toLocaleString("zh-CN") : "—";
export const /* browser时区（browserZone）保存优先使用前面的有效值，没有时采用后面的备用值：() => Intl.DateTimeFormat().resolvedOptions().timeZone || "Asia/Shanghai"。 */ browserZone = () =>
  Intl.DateTimeFormat().resolvedOptions().timeZone || "Asia/Shanghai";
export /* 检查合法性账号（validateAccount）：检查 账号（Account）。 */ function validateAccount(form, passwordKey) {
  /* 检查条件 !/^[\p{Script=Han}A-Za-z0-9]{1,30}$/u.test(form.username) 是否成立，成立时走这一分支。 */
  if (!/^[\p{Script=Han}A-Za-z0-9]{1,30}$/u.test(form.username))
    /* 抛出新建 错误（Error） 实例，当前操作按失败处理。 */
    throw new Error("用户名须为 1～30 位中文、英文字母或数字");
  /* 检查条件 Array.from(form.nickname || "").length > 30 是否成立，成立时走这一分支。 */
  if (Array.from(form.nickname || "").length > 30)
    /* 抛出新建 错误（Error） 实例，当前操作按失败处理。 */
    throw new Error("昵称最多 30 个字符");
  /* 检查条件 passwordKey 是否成立，成立时走这一分支。 */
  if (passwordKey) /* 检查 密码（Password）。 */ validatePassword(form[passwordKey], form.confirmPassword);
}
export /* 检查合法性密码（validatePassword）：检查 密码（Password）。 */ function validatePassword(password, confirmation) {
  const /* 长度（length）保存优先使用前面的有效值，没有时采用后面的备用值：Array.from(password || "").length。 */ length = Array.from(password || "").length;
  /* 检查条件 length < 8 || length > 50 是否成立，成立时走这一分支。 */
  if (length < 8 || length > 50) /* 抛出新建 错误（Error） 实例，当前操作按失败处理。 */ throw new Error("密码长度须为 8～50 个字符");
  /* 检查条件 password !== confirmation 是否成立，成立时走这一分支。 */
  if (password !== confirmation) /* 抛出新建 错误（Error） 实例，当前操作按失败处理。 */ throw new Error("两次输入的密码不一致");
}
