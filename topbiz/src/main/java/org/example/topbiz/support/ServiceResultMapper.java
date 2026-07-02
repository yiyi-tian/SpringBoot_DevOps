package org.example.topbiz.support;

import org.example.common.Result;

import java.util.Map;

public final class ServiceResultMapper {

    private ServiceResultMapper() {
    }

    @SuppressWarnings("unchecked")
    public static <T> Result<T> toResult(Map<String, Object> result) {
        if (result == null) {
            return Result.error(502, "内部服务无响应");
        }
        Object codeObj = result.get("code");
        if (codeObj == null) {
            return Result.error(502, "内部服务响应格式异常");
        }
        int code = codeObj instanceof Number number
                ? number.intValue()
                : Integer.parseInt(String.valueOf(codeObj));
        if (code != 0) {
            String message = result.get("message") != null
                    ? String.valueOf(result.get("message"))
                    : "操作失败";
            return Result.error(code, message);
        }
        return Result.ok((T) result.get("data"));
    }
}
