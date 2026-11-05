package com.asterway.habit.service.impl;

import com.asterway.common.exception.BusinessException;
import com.asterway.common.result.PageResultVO;
import com.asterway.common.utils.GrowthContextUtils;
import com.asterway.habit.dto.HabitCategorySaveDTO;
import com.asterway.habit.dto.HabitQueryDTO;
import com.asterway.habit.entity.HabitCategoryEntity;
import com.asterway.habit.mapper.HabitCategoryMapper;
import com.asterway.habit.service.HabitCategoryService;
import com.asterway.habit.vo.HabitCategoryVO;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/** 分类删除只停止新选择，不级联修改习惯及其快照。 */
@Service
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class HabitCategoryServiceImpl implements HabitCategoryService {
    private final HabitCategoryMapper mapper;
    private final GrowthContextUtils ctx;

    public HabitCategoryServiceImpl(HabitCategoryMapper mapper, GrowthContextUtils ctx) {
        this.mapper = mapper;
        this.ctx = ctx;
    }

    @Override
    public PageResultVO<HabitCategoryVO> page(HabitQueryDTO query) {
        long userId = ctx.read("habit:category:list");
        query.offset();
        if (query.getKeyword() != null && query.getKeyword().length() > 100) {
            throw BusinessException.badRequest("分类关键字过长");
        }
        return new PageResultVO<>(
                mapper.page(userId, query).stream().map(this::view).toList(),
                mapper.count(userId, query),
                query.getPage(),
                query.getSize());
    }

    @Override
    @Transactional
    public HabitCategoryVO create(HabitCategorySaveDTO dto) {
        long userId = ctx.write("habit:category:create");
        var row = new HabitCategoryEntity();
        row.setUserId(userId);
        row.setName(name(dto.name()));
        row.setSortOrder(ctx.order(dto.sortOrder()));
        unique(userId, row.getName(), null);
        row.setCreatedAt(Instant.now());
        row.setUpdatedAt(row.getCreatedAt());
        ctx.changed(mapper.insert(row));
        return view(required(userId, row.getId()));
    }

    @Override
    @Transactional
    public HabitCategoryVO update(long id, HabitCategorySaveDTO dto) {
        long userId = ctx.write("habit:category:update");
        var row = required(userId, id);
        ctx.version(row.getVersion(), dto.version());
        String name = name(dto.name());
        int order = ctx.order(dto.sortOrder());
        if (row.getName().equals(name) && row.getSortOrder() == order) {
            return view(row);
        }
        unique(userId, name, id);
        row.setName(name);
        row.setSortOrder(order);
        row.setUpdatedAt(Instant.now());
        ctx.changed(mapper.update(row));
        return view(required(userId, id));
    }

    @Override
    @Transactional
    public void delete(long id, long version) {
        long userId = ctx.write("habit:category:delete");
        var row = mapper.find(userId, id);
        if (row == null) {
            throw BusinessException.notFound();
        }
        if (row.getDeletedAt() != null) {
            return;
        }
        ctx.version(row.getVersion(), version);
        ctx.changed(mapper.softDelete(userId, id, version, Instant.now()));
    }

    private String name(String value) {
        if (value == null || value.isBlank()) {
            throw BusinessException.badRequest("分类名称不能为空");
        }
        String result = value.trim();
        if (result.codePointCount(0, result.length()) > 50) {
            throw BusinessException.badRequest("分类名称不能超过 50 个字符");
        }
        return result;
    }

    private void unique(long userId, String name, Long exceptId) {
        if (mapper.nameExists(userId, name, exceptId)) {
            throw BusinessException.conflict("已存在同名有效分类");
        }
    }

    private HabitCategoryEntity required(long userId, long id) {
        var row = mapper.find(userId, id);
        if (row == null || row.getDeletedAt() != null) {
            throw BusinessException.notFound();
        }
        return row;
    }

    private HabitCategoryVO view(HabitCategoryEntity row) {
        return new HabitCategoryVO(
                row.getId().toString(), row.getName(), row.getSortOrder(), row.getVersion());
    }
}
