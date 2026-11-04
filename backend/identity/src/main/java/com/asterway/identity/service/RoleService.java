package com.asterway.identity.service;

import com.asterway.identity.dto.MenuGrantDTO;
import com.asterway.identity.dto.PermissionGrantDTO;
import com.asterway.identity.dto.RoleUpdateDTO;
import com.asterway.identity.vo.RoleVO;
import java.util.List;

/** 三角色查询、展示信息和关系授权集中在同一链路。 */
public interface RoleService {

    /** 超管查询三个内置角色及授权回显。 */
    List<RoleVO> list();

    /** 超管查询角色详情。 */
    RoleVO get(long id);

    /** 只修改展示信息，不改变角色身份或编码。 */
    RoleVO update(long id, RoleUpdateDTO dto);

    /** 读取角色动作权限标识。 */
    List<String> permissions(long id);

    /** 读取角色菜单标识。 */
    List<String> menus(long id);

    /** 持有统一安全门锁，检查引用和角色边界后原子替换权限。 */
    RoleVO grantPermissions(long id, PermissionGrantDTO dto);

    /** 校验并补齐祖先目录，原子替换菜单授权。 */
    RoleVO grantMenus(long id, MenuGrantDTO dto);
}
