package com.asterway.habit.vo;

/** 分类返回，标识使用字符串，允许同名的已删除历史分类继续存在。 */
public record HabitCategoryVO(String id, String name, int sortOrder, long version) {}
