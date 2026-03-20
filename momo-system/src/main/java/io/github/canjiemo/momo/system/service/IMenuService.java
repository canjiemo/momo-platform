package io.github.canjiemo.momo.system.service;

import io.github.canjiemo.base.myjdbc.service.IBaseService;
import io.github.canjiemo.momo.system.dto.MenuCreateRequest;
import io.github.canjiemo.momo.system.dto.MenuDTO;
import io.github.canjiemo.momo.system.dto.MenuTreeVO;
import io.github.canjiemo.momo.system.dto.MenuUpdateRequest;

import java.util.List;

public interface IMenuService extends IBaseService {

    List<MenuTreeVO> getMenuTree();

    List<MenuTreeVO> getUserMenuTree(String userId);

    List<MenuDTO> getUserMenus(String userId);

    List<String> getUserPermissions(Long userId);

    List<MenuDTO> list();

    MenuDTO getById(Long id);

    void create(MenuCreateRequest request);

    void update(MenuUpdateRequest request);

    void delete(String... ids);
}
