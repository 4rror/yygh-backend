package com.atguigu.yygh.cmn.listener;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.atguigu.yygh.cmn.entity.Student;

public class StudentReadListener extends AnalysisEventListener<Student> {
    @Override
    public void invoke(Student student, AnalysisContext analysisContext) {
        // String sheetName = analysisContext.readSheetHolder().getSheetName();
        // 逐行读取的方法，参数1就是读取到的行数据
        System.out.println(student);
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext analysisContext) {
        // 所有行都读取完成后会执行
    }
}
