package org.example.common.message;

import java.util.Set;

/**
 * 消息域跨服务共享常量（TopBiz ↔ message-service 单一事实来源）
 */
public final class MessageConstants {

    public static final String CHANNEL_IN_APP = "IN_APP";
    public static final String CHANNEL_TENCENT_SMS = "TENCENT_SMS";
    public static final String CHANNEL_EMAIL = "EMAIL";
    public static final String CHANNEL_FEISHU = "FEISHU";
    public static final String CHANNEL_WECHAT = "WECHAT";

    public static final String SCENE_REGISTER = "REGISTER";
    public static final String SCENE_LOGIN = "LOGIN";
    public static final String SCENE_PASSWORD_RESET = "PASSWORD_RESET";

    public static final Set<String> MVP_CHANNEL_TYPES = Set.of(
            CHANNEL_IN_APP, CHANNEL_TENCENT_SMS, CHANNEL_EMAIL
    );

    public static final Set<String> RESERVED_CHANNEL_TYPES = Set.of(
            CHANNEL_FEISHU, CHANNEL_WECHAT
    );

    public static final Set<String> VERIFY_SCENES = Set.of(
            SCENE_REGISTER, SCENE_LOGIN, SCENE_PASSWORD_RESET
    );

    private MessageConstants() {
    }

    public static boolean isMvpChannel(String channelType) {
        return channelType != null && MVP_CHANNEL_TYPES.contains(channelType);
    }

    public static boolean isReservedChannel(String channelType) {
        return channelType != null && RESERVED_CHANNEL_TYPES.contains(channelType);
    }

    public static boolean isKnownChannel(String channelType) {
        return isMvpChannel(channelType) || isReservedChannel(channelType);
    }
}
