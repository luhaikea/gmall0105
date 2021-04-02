package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.UmsMember;
import com.atguigu.gmall.bean.UmsMemberReceiveAddress;

import java.util.List;

public interface UserService {
    List<UmsMember> getAllUser();

    List<UmsMemberReceiveAddress> getgetReceiveAddressByMemberId(String memberId);

    UmsMember login(UmsMember umsMember);

    void addUserToken(String token, String memberId);

    UmsMember addOauthUser(UmsMember umsMember);

    UmsMember checkOauthUser(UmsMember umsCheck);

    UmsMemberReceiveAddress getReceiveAddressById(String receiveAddressId);
}
