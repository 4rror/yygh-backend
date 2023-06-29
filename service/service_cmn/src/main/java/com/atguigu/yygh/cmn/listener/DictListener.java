package com.atguigu.yygh.cmn.listener;


import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.atguigu.yygh.cmn.mapper.DictMapper;
import com.atguigu.yygh.model.cmn.Dict;
import com.atguigu.yygh.vo.cmn.DictEeVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author haisky
 */
@Component
public class DictListener extends AnalysisEventListener<DictEeVo> {
    @Autowired
    private DictMapper dictMapper;

    // 一行一行读取
    @Override
    public void invoke(DictEeVo dictEeVo, AnalysisContext analysisContext) {
        // 调用方法添加数据库
        Dict dict = new Dict();
        BeanUtils.copyProperties(dictEeVo, dict);
        dictMapper.insert(dict);
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext analysisContext) {
    }
}
