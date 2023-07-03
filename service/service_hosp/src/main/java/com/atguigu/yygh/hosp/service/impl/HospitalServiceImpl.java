package com.atguigu.yygh.hosp.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.yygh.common.exception.YyghException;
import com.atguigu.yygh.common.util.MD5;
import com.atguigu.yygh.hosp.repository.HospitalRepository;
import com.atguigu.yygh.hosp.service.HospitalService;
import com.atguigu.yygh.hosp.service.HospitalSetService;
import com.atguigu.yygh.model.hosp.Hospital;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.Map;

/**
 * @author haisky
 */
@Service
public class HospitalServiceImpl implements HospitalService {

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private HospitalSetService hospitalSetService;

    @Override
    public void save(Map<String, Object> paramMap) {
        // 1、获取医院端的签名（这个签名是经过md5加密）
        String sign = (String) paramMap.get("sign");

        // 2、获取医院端传递的医院编号hoscode
        String hoscode = (String) paramMap.get("hoscode");
        if (StringUtils.isEmpty(hoscode)) {
            throw new YyghException(20001, "医院编号不能为空");
        }

        // 3、根据医院编号查询医院设置中的signKey（签名key），该签名key没有加密
        String signKey = hospitalSetService.getSignKey(hoscode);

        // 4、医院端传递过来的签名key和我们自己获取的签名key比较
        if (!MD5.encrypt(signKey).equals(sign)) {
            throw new YyghException(20001, "签名校验失败");
        }

        // 5、参数map转成Hospital对象
        String string = JSON.toJSONString(paramMap);
        Hospital hospital = JSON.parseObject(string, Hospital.class);
        // 6、设置医院对象默认的状态为1；logoData中的字符串替换成+
        hospital.setStatus(1);
        hospital.setLogoData(hospital.getLogoData().replaceAll(" ", "+"));


        // 7、根据hoscode去mongodb中查询该医院对象
        Hospital hp = hospitalRepository.findByHoscode(hoscode);

        // 8、如果不存在，新增操作
        if (hp == null) {
            // 添加
            hospital.setCreateTime(new Date());
            hospital.setUpdateTime(new Date());
            hospitalRepository.save(hospital);
        } else {
            // 9、如果存在，做更新操作
            hospital.setId(hp.getId());
            hospital.setCreateTime(hp.getCreateTime());
            hospital.setUpdateTime(new Date());
            hospitalRepository.save(hospital);// hospital必须有id主键
        }
    }

    @Override
    public Hospital getByHoscode(String hoscode) {
        return hospitalRepository.getHospitalByHoscode(hoscode);
    }
}
