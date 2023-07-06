package com.atguigu.yygh.cmn.controller;

import com.atguigu.yygh.cmn.service.DictService;
import com.atguigu.yygh.common.result.R;
import com.atguigu.yygh.model.cmn.Dict;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * @author haisky
 */
@RestController
@Api(description = "数据字典接口")
@RequestMapping("/admin/cmn/dict")
public class DictController {

    @Autowired
    private DictService dictService;

    @GetMapping("/findByDictCode/{dictCode}")
    public R findByDictCode(@PathVariable("dictCode") String dictCode) {
        List<Dict> dictList = dictService.findByDictCode(dictCode);
        return R.ok().data("list", dictList);
    }

    @GetMapping("/getName/{value}")
    public String getName(@PathVariable String value) {
        return dictService.getName(value, "");
    }

    @GetMapping("/getName/{value}/{dictCode}")
    public String getName(@PathVariable("value") String value, @PathVariable("dictCode") String dictCode) {
        return dictService.getName(value, dictCode);
    }

    @ApiOperation(value = "导入数据字典")
    @PostMapping("importData")
    public R importData(MultipartFile file) {
        dictService.importDictData(file);
        return R.ok();
    }

    @ApiOperation(value = "导出字典数据")
    @GetMapping(value = "/exportData")
    public void exportData(HttpServletResponse response) {
        dictService.exportData(response);
    }

    @ApiOperation("根据数据id查询子数据列表")
    @GetMapping("/findChildData/{id}")
    public R findChildData(@PathVariable Long id) {
        List<Dict> list = dictService.findChildData(id);
        return R.ok().data("list", list);
    }
}
