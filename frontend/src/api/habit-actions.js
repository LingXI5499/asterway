import { ElMessage } from "element-plus";
import { habitApi, signalChange } from "./personal";
import { canGrow, today } from "./growth";
import { confirmAction } from "./ui";

const idOf = (row) => row.habitId || row.id;
export async function checkHabit(row) {
  if (!canGrow("habit:checkin:create")) throw new Error("没有今日打卡权限");
  const current =
    row.version !== undefined ? row : await habitApi.detail(idOf(row));
  const result = await habitApi.checkin(current);
  signalChange("habit");
  ElMessage.success("今日打卡已记录");
  return result;
}
export async function cancelHabitToday(row) {
  if (!canGrow("habit:checkin:cancel") || !canGrow("habit:checkin:read"))
    throw new Error("撤销需要打卡读取与撤销权限");
  const id = idOf(row),
    date = today();
  const result = await habitApi.checkins(id, {
    dateFrom: date,
    dateTo: date,
    cancelled: false,
    size: 20,
  });
  const checkin = row.checkinId
    ? result.items.find((item) => item.id === row.checkinId)
    : result.items[0];
  if (!checkin) throw new Error("今日打卡状态已变化，请刷新后重试");
  await confirmAction(
    "撤销「" +
      row.name +
      "」今天的打卡？撤销后会按今天最新规则判断是否应做，过去记录不变。",
  );
  const value = await habitApi.cancel(id, checkin);
  signalChange("habit");
  ElMessage.success("今日打卡已撤销");
  return value;
}
