package com.atguigu.yygh.cmn.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * @author haisky
 */
@FeignClient("service-cmn")
public interface DictFeignClient {

    @GetMapping("/admin/cmn/dict/getName/{value}")
    String getName(@PathVariable String value);

    @GetMapping("/admin/cmn/dict/getName/{value}/{dictCode}")
    String getName(@PathVariable("value") String value, @PathVariable("dictCode") String dictCode);

}
