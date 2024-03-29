package com.atguigu.yygh.cmn.service.impl;

import com.alibaba.excel.EasyExcel;
import com.atguigu.yygh.cmn.listener.DictListener;
import com.atguigu.yygh.cmn.mapper.DictMapper;
import com.atguigu.yygh.cmn.service.DictService;
import com.atguigu.yygh.model.cmn.Dict;
import com.atguigu.yygh.vo.cmn.DictEeVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author haisky
 */
@Slf4j
@Service
public class DictServiceImpl extends ServiceImpl<DictMapper, Dict> implements DictService {

    // 根据数据id查询子数据列表
    // @Override
    // public List<Dict> findChildData(Long id) {
    //     QueryWrapper<Dict> wrapper = new QueryWrapper<>();
    //     wrapper.eq("parent_id", id);
    //     List<Dict> dictList = baseMapper.selectList(wrapper);
    //     // 向list集合每个dict对象中设置hasChildren
    //     for (Dict dict : dictList) {
    //         Long dictId = dict.getId();
    //         boolean isChild = this.isChildren(dictId);
    //         dict.setHasChildren(isChild);
    //     }
    //     return dictList;
    // }
    //
    // // 判断id下面是否有子节点
    // private boolean isChildren(Long id) {
    //     QueryWrapper<Dict> wrapper = new QueryWrapper<>();
    //     wrapper.eq("parent_id", id);
    //     Integer count = baseMapper.selectCount(wrapper);
    //     return count > 0;
    // }

    @Autowired
    private DictListener dictListener;

    @Override
    public void importDictData(MultipartFile file) {
        try {
            EasyExcel.read(file.getInputStream(), DictEeVo.class, dictListener).sheet().doRead();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getName(String value, String dictCode) {
        if (StringUtils.isEmpty(dictCode)) {
            QueryWrapper<Dict> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("value", value);
            List<Dict> dictList = baseMapper.selectList(queryWrapper);
            if (dictList.size() > 1) {
                return "该value = " + value + "值不唯一";
            }
            return dictList.size() > 0 ? dictList.get(0).getName() : "该value没有对应的名称";
        } else {
            QueryWrapper<Dict> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("value", value).eq("parent_id", getDictByDictCode(dictCode).getId());
            return baseMapper.selectOne(queryWrapper).getName();
        }
    }

    @Override
    public List<Dict> findByDictCode(String dictCode) {
        // 通过dict先查询到记录
        QueryWrapper<Dict> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("parent_id", getDictByDictCode(dictCode).getId());
        return baseMapper.selectList(queryWrapper);
    }

    private Dict getDictByDictCode(String dictCode) {
        QueryWrapper<Dict> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("dict_code", dictCode);
        return baseMapper.selectOne(queryWrapper);
    }

    @CacheEvict(value = "dict", allEntries = true)
    @Override
    public void exportData(HttpServletResponse response) {
        try {
            response.setContentType("application/vnd.ms-excel");
            response.setCharacterEncoding("utf-8");
            // 这里URLEncoder.encode可以防止中文乱码 当然和easyexcel没有关系
            String fileName = URLEncoder.encode("数据字典", "UTF-8");
            response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".xlsx");
            List<Dict> dictList = baseMapper.selectList(null);
            List<DictEeVo> dictVoList = new ArrayList<>(dictList.size());
            for (Dict dict : dictList) {
                DictEeVo dictVo = new DictEeVo();
                BeanUtils.copyProperties(dict, dictVo);
                dictVoList.add(dictVo);
            }
            EasyExcel
                    .write(response.getOutputStream(), DictEeVo.class)
                    .sheet("数据字典")
                    .doWrite(dictVoList);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Cacheable("dict")
    @Override
    public List<Dict> findChildData(Long id) {
        List<Dict> all = baseMapper.selectList(new QueryWrapper<>());

        Dict dict = all.stream().filter(i -> Objects.equals(id, i.getId())).findFirst().orElse(null);
        List<Dict> collect = all.stream().filter(i -> Objects.equals(i.getParentId(), Objects.requireNonNull(dict).getId())).collect(Collectors.toList());
        return buildDictTree(collect, dict);
    }


    private List<Dict> buildDictTree(List<Dict> all, Dict dict) {
        List<Dict> list = new ArrayList<>();

        all.forEach(item -> {
            if (Objects.equals(item.getParentId(), dict.getId())) {
                list.add(item);
            }
        });

        if (list.size() > 0) {
            dict.setHasChildren(true);
            dict.getParam().put("children", list);
            for (Dict d : list) {
                Integer count = baseMapper.selectCount(new QueryWrapper<Dict>().eq("parent_id", d.getId()));
                if (count > 0) {
                    d.setHasChildren(true);
                }
                buildDictTree(all, d);
            }
        }

        return list;
    }
}
