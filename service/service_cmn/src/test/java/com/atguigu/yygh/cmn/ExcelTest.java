package com.atguigu.yygh.cmn;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.atguigu.yygh.cmn.entity.Student;
import com.atguigu.yygh.cmn.listener.StudentReadListener;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@SpringBootTest
public class ExcelTest {

    public List<Student> dataList() {
        List<Student> list = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Student student = new Student(i, UUID.randomUUID().toString().replaceAll("-", ""));
            list.add(student);
        }
        return list;
    }

    @Test
    public void testWrite() {
        List<Student> list = this.dataList();
        EasyExcel.write("C:\\Users\\ygx\\Desktop\\学生列表.xlsx", Student.class)
                .sheet("学生列表")
                .doWrite(list);
    }

    @Test
    public void testWrite2() {
        List<Student> students = this.dataList();

        ExcelWriter excelWriter =
                EasyExcel.write("C:\\Users\\ygx\\Desktop\\学生列表2.xlsx", Student.class)
                        .build();
        WriteSheet writeSheet = EasyExcel.writerSheet("我的学生列表").build();
        excelWriter.write(students, writeSheet);
        excelWriter.finish();
    }

    @Test
    public void testRead() {
        String file = "C:\\Users\\ygx\\Desktop\\学生列表.xlsx";

        // 读监听器readListener（逐行读取excel）
        EasyExcel.read(file, Student.class, new StudentReadListener())
                .sheet("学生列表")
                .doRead();
    }
}
