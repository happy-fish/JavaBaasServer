package com.staryet.baas.user.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.staryet.baas.common.entity.SimpleCode;
import com.staryet.baas.common.entity.SimpleError;
import com.staryet.baas.common.entity.SimpleResult;
import com.staryet.baas.common.service.MasterService;
import com.staryet.baas.common.util.JSONUtil;
import com.staryet.baas.user.entity.BaasAuth;
import com.staryet.baas.user.entity.BaasUser;
import com.staryet.baas.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Staryet on 15/8/13.
 */
@RestController
@RequestMapping(value = "/api/user")
public class UserController {

    @Autowired
    private UserService userService;
    @Autowired
    private MasterService masterService;
    @Autowired
    private HttpServletRequest request;
    @Autowired
    private JSONUtil jsonUtil;

    /**
     * 注册用户
     *
     * @param body 用户信息
     * @return 注册结果及用户id
     * @throws SimpleError
     */
    @RequestMapping(value = "", method = RequestMethod.POST)
    @ResponseBody
    public SimpleResult register(@RequestHeader(value = "JB-AppId") String appId,
                                 @RequestHeader(value = "JB-Plat") String plat,
                                 @RequestBody String body) {
        BaasUser register = jsonUtil.readValue(body, BaasUser.class);
        BaasUser user = userService.register(appId, plat, register);
        SimpleResult result = SimpleResult.success();
        result.putData("_id", user.getId());
        result.putData("createdAt", user.getCreatedAt());
        result.putData("sessionToken", user.getSessionToken());
        return result;
    }

    /**
     * 绑定社交平台
     *
     * @param appId    应用id
     * @param authData 社交平台信息
     * @param id       用户id
     * @param platform 社交平台名称
     * @return 请求结果
     * @throws SimpleError
     */
    @RequestMapping(value = "/{id}/binding/{platform}", method = RequestMethod.POST)
    @ResponseBody
    public SimpleResult bindingSns(@RequestHeader(value = "JB-AppId") String appId,
                                   @RequestHeader(value = "JB-Plat") String plat,
                                   @RequestBody String authData,
                                   @PathVariable String id,
                                   @PathVariable String platform) {
        //处理权限
        boolean isMaster = masterService.isMaster(request);
        BaasUser currentUser = userService.getCurrentUser(appId, plat, request);
        BaasAuth auth = jsonUtil.readValue(authData, BaasAuth.class);
        if (StringUtils.isEmpty(auth.getUid()) || StringUtils.isEmpty(auth.getAccessToken())) {
            //授权信息不足
            throw new SimpleError(SimpleCode.USER_AUTH_ERROR);
        }
        userService.bindingSns(appId, plat, id, platform, auth, currentUser, isMaster);
        return SimpleResult.success();
    }

    /**
     * 解绑社交平台
     *
     * @param appId    应用id
     * @param id       用户id
     * @param platform 社交平台名称
     * @return 请求结果
     * @throws SimpleError
     */
    @RequestMapping(value = "/{id}/release/{platform}", method = RequestMethod.DELETE)
    @ResponseBody
    public SimpleResult releaseSns(@RequestHeader(value = "JB-AppId") String appId,
                                   @RequestHeader(value = "JB-Plat") String plat,
                                   @PathVariable String id,
                                   @PathVariable String platform) {
        //处理权限
        boolean isMaster = masterService.isMaster(request);
        BaasUser currentUser = userService.getCurrentUser(appId, plat, request);
        userService.releaseSns(appId, plat, id, platform, currentUser, isMaster);
        return SimpleResult.success();
    }

    /**
     * 更新用户信息
     *
     * @param body 用户信息
     * @return 请求结果
     * @throws SimpleError
     */
    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
    @ResponseBody
    public SimpleResult update(@RequestHeader(value = "JB-AppId") String appId,
                               @RequestHeader(value = "JB-Plat") String plat,
                               @RequestBody String body,
                               @PathVariable String id) {
        BaasUser user = jsonUtil.readValue(body, BaasUser.class);
        //处理权限
        boolean isMaster = masterService.isMaster(request);
        BaasUser currentUser = userService.getCurrentUser(appId, plat, request);
        userService.update(appId, plat, id, user, currentUser, isMaster);
        return SimpleResult.success();
    }

    @RequestMapping(value = "/{id}/updatePassword", method = RequestMethod.PUT)
    @ResponseBody
    public SimpleResult updatePassword(@RequestHeader(value = "JB-AppId") String appId,
                                       @RequestHeader(value = "JB-Plat") String plat,
                                       @RequestBody String body,
                                       @PathVariable String id) {
        Map<String, String> data = jsonUtil.readValue(body, new TypeReference<HashMap<String, String>>() {
        });
        String oldPassword = data.get("oldPassword");
        String newPassword = data.get("newPassword");
        BaasUser currentUser = userService.getCurrentUser(appId, plat, request);
        BaasUser user = userService.updatePassword(appId, plat, id, oldPassword, newPassword, currentUser);
        SimpleResult result = SimpleResult.success();
        result.putData("sessionToken", user.getSessionToken());
        return result;
    }

    /**
     * 用户登录
     *
     * @param username 用户名
     * @param password 密码
     * @return 请求结果
     * @throws SimpleError
     */
    @RequestMapping(value = "/login", method = RequestMethod.GET)
    @ResponseBody
    public BaasUser login(@RequestHeader(value = "JB-AppId") String appId,
                          @RequestHeader(value = "JB-Plat") String plat,
                          @RequestParam(required = true) String username,
                          @RequestParam(required = true) String password) {
        return userService.login(appId, plat, username, password);
    }

    @RequestMapping(value = "/loginWithSns/{platform}", method = RequestMethod.POST)
    @ResponseBody
    public BaasUser loginWithSns(@RequestHeader(value = "JB-AppId") String appId,
                                 @RequestHeader(value = "JB-Plat") String plat,
                                 @RequestBody String authData,
                                 @PathVariable String platform) {
        BaasAuth auth = jsonUtil.readValue(authData, BaasAuth.class);
        if (StringUtils.isEmpty(auth.getUid()) || StringUtils.isEmpty(auth.getAccessToken())) {
            //授权信息不足
            throw new SimpleError(SimpleCode.USER_AUTH_ERROR);
        }
        return userService.loginWithSns(appId, plat, platform, auth);
    }

    /**
     * 重置sessionToken
     *
     * @param appId 应用id
     * @param plat  平台
     * @param id    用户id
     * @return 结果
     */
    @RequestMapping(value = "/{id}/resetSessionToken", method = RequestMethod.PUT)
    @ResponseBody
    public SimpleResult resetSessionToken(@RequestHeader(value = "JB-AppId") String appId,
                                          @RequestHeader(value = "JB-Plat") String plat,
                                          @PathVariable String id) {
        userService.resetSessionToken(appId, plat, id);
        return SimpleResult.success();
    }

}
