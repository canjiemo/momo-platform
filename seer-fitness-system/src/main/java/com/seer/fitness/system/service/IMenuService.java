package com.seer.fitness.system.service;

import com.seer.fitness.system.dto.MenuCreateRequest;
import com.seer.fitness.system.dto.MenuDTO;
import com.seer.fitness.system.dto.MenuTreeVO;
import com.seer.fitness.system.dto.MenuUpdateRequest;
import io.github.canjiemo.base.myjdbc.service.IBaseService;

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
