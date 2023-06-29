package com.atguigu.yygh.cmn.service;

import com.atguigu.yygh.model.cmn.Dict;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

public interface DictService extends IService<Dict> {

    /**
     * 导出数据
     */
    void exportData(HttpServletResponse response);

    /**
     * 构建数据
     *
     * @param id 以id为parent_id
     * @return 返回构造完成的列表
     */
    List<Dict> findChildData(Long id);

    void importDictData(MultipartFile file);
}
