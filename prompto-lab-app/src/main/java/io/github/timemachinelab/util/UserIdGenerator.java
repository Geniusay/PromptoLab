package io.github.timemachinelab.util;

import javax.servlet.http.HttpServletRequest;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;

/**
 * 用户ID生成工具类
 * 根据请求的IP地址和浏览器信息生成唯一的用户指纹
 */
public class UserIdGenerator {

    /**
     * 根据HTTP请求生成用户唯一指纹
     * 
     * @param request HTTP请求对象
     * @return 用户唯一指纹字符串
     */
    public static String generateUserId(HttpServletRequest request) {
        // 获取客户端IP地址
        String clientIp = getClientIpAddress(request);
        
        // 获取User-Agent信息
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null) {
            userAgent = "unknown";
        }
        
        // 获取Accept-Language信息
        String acceptLanguage = request.getHeader("Accept-Language");
        if (acceptLanguage == null) {
            acceptLanguage = "unknown";
        }
        
        // 获取Accept-Encoding信息
        String acceptEncoding = request.getHeader("Accept-Encoding");
        if (acceptEncoding == null) {
            acceptEncoding = "unknown";
        }
        
        // 组合指纹信息
        String fingerprint = clientIp + "|" + userAgent + "|" + acceptLanguage + "|" + acceptEncoding;
        
        // 生成MD5哈希值作为用户ID
        return generateMD5Hash(fingerprint);
    }
    
    /**
     * 获取客户端真实IP地址
     * 
     * @param request HTTP请求对象
     * @return 客户端IP地址
     */
    private static String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            // 多个IP时取第一个
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        String proxyClientIp = request.getHeader("Proxy-Client-IP");
        if (proxyClientIp != null && !proxyClientIp.isEmpty() && !"unknown".equalsIgnoreCase(proxyClientIp)) {
            return proxyClientIp;
        }
        
        String wlProxyClientIp = request.getHeader("WL-Proxy-Client-IP");
        if (wlProxyClientIp != null && !wlProxyClientIp.isEmpty() && !"unknown".equalsIgnoreCase(wlProxyClientIp)) {
            return wlProxyClientIp;
        }
        
        String httpClientIp = request.getHeader("HTTP_CLIENT_IP");
        if (httpClientIp != null && !httpClientIp.isEmpty() && !"unknown".equalsIgnoreCase(httpClientIp)) {
            return httpClientIp;
        }
        
        String httpXForwardedFor = request.getHeader("HTTP_X_FORWARDED_FOR");
        if (httpXForwardedFor != null && !httpXForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(httpXForwardedFor)) {
            return httpXForwardedFor;
        }
        
        // 如果都获取不到，返回request.getRemoteAddr()
        return request.getRemoteAddr();
    }
    
    /**
     * 生成MD5哈希值
     * 
     * @param input 输入字符串
     * @return MD5哈希值
     */
    private static String generateMD5Hash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
            
            // 将字节数组转换为十六进制字符串
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // 如果MD5算法不可用，使用简单的哈希码
            return String.valueOf(input.hashCode());
        }
    }
}