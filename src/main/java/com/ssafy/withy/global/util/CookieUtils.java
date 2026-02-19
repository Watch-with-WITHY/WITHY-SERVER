package com.ssafy.withy.global.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;
import java.util.Optional;

public class CookieUtils {

    public static Optional<Cookie> getCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        // [수정] length > 0 체크 삭제 (IDE 경고 해결)
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    return Optional.of(cookie);
                }
            }
        }
        return Optional.empty();
    }

    public static void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(true); // HTTPS 필수
        cookie.setMaxAge(maxAge);
        cookie.setAttribute("SameSite", "None"); // 크롬 정책 대응
        response.addCookie(cookie);
    }

    public static void deleteCookie(HttpServletRequest request, HttpServletResponse response, String name) {
        Cookie[] cookies = request.getCookies();
        // [수정] length > 0 체크 삭제 (IDE 경고 해결)
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    cookie.setValue("");
                    cookie.setPath("/");
                    cookie.setMaxAge(0);
                    response.addCookie(cookie);
                }
            }
        }
    }

    // [수정] Deprecated 된 Spring Utils 대신 순수 자바 직렬화 사용 (경고 해결)
    public static String serialize(Object object) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(object);
            return Base64.getUrlEncoder().encodeToString(bos.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("쿠키 직렬화 실패", e);
        }
    }

    // [수정] Deprecated 된 Spring Utils 대신 순수 자바 역직렬화 사용 (경고 해결)
    public static <T> T deserialize(Cookie cookie, Class<T> cls) {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(
                Base64.getUrlDecoder().decode(cookie.getValue()));
             ObjectInputStream ois = new ObjectInputStream(bis)) {
            return cls.cast(ois.readObject());
        } catch (Exception e) {
            throw new RuntimeException("쿠키 역직렬화 실패", e);
        }
    }
}