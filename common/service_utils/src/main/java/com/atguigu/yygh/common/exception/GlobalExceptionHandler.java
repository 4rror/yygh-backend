package com.atguigu.yygh.common.exception;

import com.atguigu.yygh.common.result.R;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * @author haisky
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(YyghException.class)
    public R error(YyghException e) {
        e.printStackTrace();
        return R.error().message(e.getMsg()).code(e.getCode());
    }

    @ExceptionHandler(value = Exception.class)
    public R handle(Exception e) {
        e.printStackTrace();
        return R.error().message(e.getMessage());
    }

    @ExceptionHandler(ArithmeticException.class)
    public R handle(ArithmeticException e) {
        e.printStackTrace();
        return R.error().message(e.getMessage());
    }

    @ExceptionHandler(NullPointerException.class)
    public R handle(NullPointerException e) {
        e.printStackTrace();
        return R.error().message("空指针异常");
    }
}
