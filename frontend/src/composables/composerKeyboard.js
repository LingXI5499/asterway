// 输入法确认文字时交还浏览器处理，只有普通回车才触发发送。
export function composerEnterAction(event) {
  if (event.key !== "Enter" || event.isComposing || event.keyCode === 229) return "native";
  if (event.ctrlKey) return "newline";
  return event.shiftKey || event.altKey || event.metaKey ? "native" : "send";
}

// 换行插在光标处；有选中文字时，使用换行替换选区。
export function insertComposerNewline(value, start, end = start) {
  return { value: value.slice(0, start) + "\n" + value.slice(end), caret: start + 1 };
}
