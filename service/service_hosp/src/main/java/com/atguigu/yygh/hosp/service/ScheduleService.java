package com.atguigu.yygh.hosp.service;

import java.util.Map;

/**
 * @author haisky
 */
public interface ScheduleService {
    /**
     * 上传排班信息
     *
     * @param paramMap
     */
    void save(Map<String, Object> paramMap);
}
