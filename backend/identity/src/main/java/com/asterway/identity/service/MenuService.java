package com.asterway.identity.service;

import com.asterway.identity.dto.MenuSaveDTO;
import com.asterway.identity.vo.MenuVO;
import java.util.List;

/** 目录、页面和按钮的同一管理链路。 */
public interface MenuService {

    /** 超管读取平铺菜单。 */
    List<MenuVO> list();

    /** 超管读取管理菜单树。 */
    List<MenuVO> tree();

    /** 根据当前身份过滤菜单、权限及完整父链。 */
    List<MenuVO> currentTree();

    /** 超管读取节点。 */
    MenuVO get(long id);

    /** 校验父链与组件白名单后新增。 */
    MenuVO create(MenuSaveDTO dto);

    /** 比较版本并拒绝循环父链。 */
    MenuVO update(long id, MenuSaveDTO dto);

    /** 只删除未引用、无有效子节点的扩展菜单。 */
    void delete(long id, long version);
}
