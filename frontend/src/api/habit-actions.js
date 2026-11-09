/* 引入 element-plus 提供的组件、状态函数或请求方法。 */
/* 封装习惯actions接口请求，供页面复用同一套路径、参数和响应处理。 */
import { ElMessage } from "element-plus";
/* 引入 ./personal 提供的组件、状态函数或请求方法。 */
import { habitApi, signalChange } from "./personal";
/* 引入 ./growth 提供的组件、状态函数或请求方法。 */
import { canGrow, today } from "./growth";
/* 引入 ./ui 提供的组件、状态函数或请求方法。 */
import { confirmAction } from "./ui";

const /* 编号Of（idOf）保存优先使用前面的有效值，没有时采用后面的备用值：(row) => row.habitId || row.id。 */ idOf = (row) => row.habitId || row.id;
export /* 核对习惯（checkHabit）：核对 习惯（Habit）。 */ async function checkHabit(row) {
  /* 检查当前用户是否具有需要的权限：!canGrow("habit:checkin:create")，成立时走这一分支。 */
  if (!canGrow("habit:checkin:create")) /* 抛出新建 错误（Error） 实例，当前操作按失败处理。 */ throw new Error("没有今日打卡权限");
  const /* 当前（current）保存根据前面的条件，从两个候选结果中选择一个：row.version !== undefined ? row : await habitApi.detail(idOf(row))。 */ current =
    row.version !== undefined ? row : await habitApi.detail(idOf(row));
  const /* 结果（result）保存等待 核对 in 完成后取得结果。 */ result = await habitApi.checkin(current);
  /* 调用 signalChange 完成取消信号修改处理。 */
  signalChange("habit");
  /* 调用 ElMessage.success 完成成功处理。 */
  ElMessage.success("今日打卡已记录");
  /* 返回结果（result），当前函数到此结束。 */
  return result;
}
export /* 取消习惯今天（cancelHabitToday）：取消 习惯今天（HabitToday）。 */ async function cancelHabitToday(row) {
  /* 检查当前用户是否具有需要的权限：!canGrow("habit:checkin:cancel") || !canGrow("habit:checkin:read")，成立时走这一分支。 */
  if (!canGrow("habit:checkin:cancel") || !canGrow("habit:checkin:read"))
    /* 抛出新建 错误（Error） 实例，当前操作按失败处理。 */
    throw new Error("撤销需要打卡读取与撤销权限");
  const /* 编号（id）保存调用 idOf 完成编号Of处理。 */ id = idOf(row),
    /* 日期（date）保存调用 today 完成今天处理。 */
    date = today();
  const /* 结果（result）保存等待 核对 ins 完成后取得结果。 */ result = await habitApi.checkins(id, {
    /* 日期来源（dateFrom）字段使用日期（date）。 */
    dateFrom: date,
    /* 日期To（dateTo）字段使用日期（date）。 */
    dateTo: date,
    /* 已取消（cancelled）字段使用假，表示条件不成立或开关关闭。 */
    cancelled: false,
    /* 每页条数（size）字段使用固定数值 20。 */
    size: 20,
  });
  const /* 打卡（checkin）保存根据前面的条件，从两个候选结果中选择一个：row.checkinId ? result.items.find((item) => item.id === row.checkinId) : result.items[0]。 */ checkin = row.checkinId
    ? result.items.find((item) => item.id === row.checkinId)
    : result.items[0];
  /* 检查条件 !checkin 是否成立，成立时走这一分支。 */
  if (!checkin) /* 抛出新建 错误（Error） 实例，当前操作按失败处理。 */ throw new Error("今日打卡状态已变化，请刷新后重试");
  /* 等待确认 操作（Action）。 */
  await confirmAction(
    "撤销「" +
      row.name +
      "」今天的打卡？撤销后会按今天最新规则判断是否应做，过去记录不变。",
  );
  const /* 值（value）保存等待 调用 habitApi.cancel 完成取消处理 完成后取得结果。 */ value = await habitApi.cancel(id, checkin);
  /* 调用 signalChange 完成取消信号修改处理。 */
  signalChange("habit");
  /* 调用 ElMessage.success 完成成功处理。 */
  ElMessage.success("今日打卡已撤销");
  /* 返回值（value），当前函数到此结束。 */
  return value;
}
