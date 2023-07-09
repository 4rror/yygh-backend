package com.atguigu.yygh.user.service.impl;

import com.atguigu.yygh.common.exception.YyghException;
import com.atguigu.yygh.common.jwt.JwtHelper;
import com.atguigu.yygh.common.result.ResultCode;
import com.atguigu.yygh.enums.AuthStatusEnum;
import com.atguigu.yygh.model.user.UserInfo;
import com.atguigu.yygh.user.mapper.UserInfoMapper;
import com.atguigu.yygh.user.service.UserInfoService;
import com.atguigu.yygh.vo.user.LoginVo;
import com.atguigu.yygh.vo.user.UserAuthVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author haisky
 */
@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements UserInfoService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void userAuth(Long userId, UserAuthVo userAuthVo) {
        // 根据用户id查询用户信息
        UserInfo userInfo = baseMapper.selectById(userId);

        // 赋值
        userInfo.setName(userAuthVo.getName());
        userInfo.setCertificatesType(userAuthVo.getCertificatesType());
        userInfo.setCertificatesNo(userAuthVo.getCertificatesNo());
        userInfo.setCertificatesUrl(userAuthVo.getCertificatesUrl());
        userInfo.setAuthStatus(AuthStatusEnum.AUTH_RUN.getStatus());// 认证中（就是已提交）

        // 进行信息更新
        baseMapper.updateById(userInfo);
    }

    @Override
    public Map<String, Object> login(LoginVo loginVo) {
        String phone = loginVo.getPhone();
        String code = loginVo.getCode();
        // 手机号 + 短信验证码登陆接口的需求：
        // 1. 手机号，验证码 判断非空
        if (StringUtils.isEmpty(phone)) {
            throw new YyghException(ResultCode.ERROR, "手机号不能为空");
        }
        if (StringUtils.isEmpty(code)) {
            throw new YyghException(ResultCode.ERROR, "验证码不能为空");
        }

        // 2. 判断验证码是否正确（用户输入的验证码和真正发送的验证码比较），校验失败，抛出自定义异常，提示验证码不正确
        String codeFromRedis = stringRedisTemplate.opsForValue().get(phone);
        if (StringUtils.isEmpty(codeFromRedis)) {
            throw new YyghException(ResultCode.ERROR, "验证码已过期，请重新发送");
        }
        if (!code.equals(codeFromRedis)) {
            throw new YyghException(ResultCode.ERROR, "验证码不正确");
        }

        // 3. 如果验证码正确，接下来判断该手机号是否存在，如果不存在，自动注册（authStatus=0，status=1（正常状态））
        QueryWrapper<UserInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("phone", phone);
        UserInfo userInfo = baseMapper.selectOne(queryWrapper);
        // 注册
        if (userInfo == null) {
            userInfo = new UserInfo();
            userInfo.setPhone(phone);
            userInfo.setAuthStatus(AuthStatusEnum.NO_AUTH.getStatus());
            // 正常状态
            userInfo.setStatus(1);
            int insert = baseMapper.insert(userInfo);
            if (insert < 0) {
                throw new YyghException(ResultCode.ERROR, "自动注册失败");
            }
        }

        if (userInfo.getStatus() == 0) {
            throw new YyghException(ResultCode.ERROR, "用户已被锁定，不允许登陆");
        }

        // 4. 如果存在，判断该用户的status状态是否被锁定，抛出自定义异常，提示用户被锁定
        // String name = userInfo.getName();
        // if (StringUtils.isEmpty(name)) {
        //     name = userInfo.getNickName();
        //     if (StringUtils.isEmpty(name)) {
        //         name = userInfo.getPhone();
        //     }
        // }
        // String token = JwtHelper.createToken(userInfo.getId(), name);
        //
        // Map<String, Object> map = new HashMap<>();
        // map.put("token", token);
        // map.put("name", name);

        Map<String, Object> map = this.getMap(userInfo);

        // 5. 准备返回值：name + token
        //    name: 右上角显示的内容，有线取userInfo中的name，如果为空取出nick_name，依然为空最后取出phone
        //    token: 当前用户登陆成功后，服务端办法的jwt格式的令牌，该令牌是加密的，令牌中会存储用户的一部分信息，例如：用户的id，用户的name
        // 前端收到name和token后，存储在浏览器的cookie中
        // 登陆成功后，前端每次发起的请求，都会主动从cookie中获取token令牌，放在请求头中，一些传递给服务端接口
        // 服务端接受到该令牌之后，可以校验，可以解析其中的信息
        return map;
    }

    @Override
    public UserInfo selectByOpenid(String openid) {
        QueryWrapper<UserInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("openid", openid);
        return baseMapper.selectOne(queryWrapper);
    }

    /**
     * 为微信用户登陆绑定手机号
     *
     * @param loginVo 登陆参数
     */
    @Override
    public Map<String, Object> bundle(LoginVo loginVo) {
        // 1. 非空校验
        String code = loginVo.getCode();
        String phone = loginVo.getPhone();
        String openid = loginVo.getOpenid();

        if (StringUtils.isEmpty(code)) {
            throw new YyghException(ResultCode.ERROR, "验证码不能为空");
        }
        if (StringUtils.isEmpty(phone)) {
            throw new YyghException(ResultCode.ERROR, "手机号不能为空");
        }
        if (StringUtils.isEmpty(openid)) {
            throw new YyghException(ResultCode.ERROR, "openid为null");
        }

        // 2. 短信验证码校验
        String codeFromRedis = stringRedisTemplate.opsForValue().get(phone);
        if (StringUtils.isEmpty(codeFromRedis)) {
            throw new YyghException(ResultCode.ERROR, "验证码不存在，请重新发送");
        }
        if (!code.equals(codeFromRedis)) {
            throw new YyghException(ResultCode.ERROR, "验证码错误");
        }

        // 3. 根据openid查询微信用户
        UserInfo userInfoByOpenid = this.selectByOpenid(openid);

        // 4. 根据phone查询手机号用户
        UserInfo userInfoByPhone = this.selectByPhone(phone);

        // 5. 判断4的返回值为null，说明，该手机号在数据库中不存在，可以直接绑定
        if (userInfoByPhone == null) {
            userInfoByOpenid.setPhone(phone);
            userInfoByOpenid.setUpdateTime(new Date());
            baseMapper.updateById(userInfoByOpenid);

            // 判断用户是否被锁定
            if (userInfoByOpenid.getStatus() == 0) {
                throw new YyghException(ResultCode.ERROR, "用户被锁定");
            }
            return this.getMap(userInfoByOpenid);
        } else {
            // 6. 手机号已经存在
            // 6.1 已存在手机号的对象是否有openid，有就抛出异常
            if (!StringUtils.isEmpty(userInfoByPhone.getOpenid())) {
                throw new YyghException(ResultCode.ERROR, "手机号已被占用");
            }
            // 6.2 没有openid，删除没有openid的记录，为微信用户绑定phone
            baseMapper.deleteById(userInfoByPhone.getId());
            userInfoByOpenid.setPhone(phone);
            userInfoByOpenid.setUpdateTime(new Date());
            baseMapper.updateById(userInfoByOpenid);

            return this.getMap(userInfoByOpenid);
        }
    }

    @Override
    public UserInfo selectByPhone(String phone) {
        QueryWrapper<UserInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("phone", phone);
        return baseMapper.selectOne(queryWrapper);
    }

    private Map<String, Object> getMap(UserInfo userInfo) {
        String name = userInfo.getName();
        if (StringUtils.isEmpty(name)) {
            name = userInfo.getNickName();
            if (StringUtils.isEmpty(name)) {
                name = userInfo.getPhone();
            }
        }
        String token = JwtHelper.createToken(userInfo.getId(), name);
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("token", token);
        return map;
    }
}
