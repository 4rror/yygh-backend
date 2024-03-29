package com.atguigu.yygh.common.exception;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author haisky
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class YyghException extends RuntimeException {
    @ApiModelProperty(value = "状态码")
    private Integer code;
    @ApiModelProperty(value = "错误消息")
    private String msg;
}