package com.atguigu.yygh.cmn.entity;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Student {
    @ExcelProperty(index = 0, value = "学生编号")
    private Integer stuid;
    @ExcelProperty(index = 1, value = "学生姓名")
    private String stuname;
}