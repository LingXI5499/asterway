import { ElMessage, ElMessageBox } from "element-plus";
export const report = (error) => {
  if (!["cancel", "close"].includes(error))
    ElMessage.error(error.message || "操作失败");
};
export const confirmAction = (message, confirmButtonText = "确认操作") =>
  ElMessageBox.confirm(message, "确认操作", {
    type: "warning",
    confirmButtonText,
    cancelButtonText: "取消",
  });
export const dateText = (value) =>
  value ? new Date(value).toLocaleString("zh-CN") : "—";
export const browserZone = () =>
  Intl.DateTimeFormat().resolvedOptions().timeZone || "Asia/Shanghai";
export function validateAccount(form, passwordKey) {
  if (!/^[\p{Script=Han}A-Za-z0-9]{1,30}$/u.test(form.username))
    throw new Error("用户名须为 1～30 位中文、英文字母或数字");
  if (Array.from(form.nickname || "").length > 30)
    throw new Error("昵称最多 30 个字符");
  if (passwordKey) validatePassword(form[passwordKey], form.confirmPassword);
}
export function validatePassword(password, confirmation) {
  const length = Array.from(password || "").length;
  if (length < 8 || length > 50) throw new Error("密码长度须为 8～50 个字符");
  if (password !== confirmation) throw new Error("两次输入的密码不一致");
}
