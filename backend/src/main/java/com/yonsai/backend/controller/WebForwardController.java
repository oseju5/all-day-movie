package com.yonsai.backend.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 프론트엔드 React Router(SPA) 라우팅을 위한 포워딩 컨트롤러.
 * 
 * 브라우저 새로고침이나 직접 URL 입력 시 백엔드(Spring Boot)에 없는 경로로 접근하게 되어 
 * 404 에러가 발생하는 것을 방지하기 위해, 지정된 프론트엔드 경로를 index.html로 포워딩합니다.
 *
 * @author ohseju
 * @since : 2026-03-23
 */
@Controller
public class WebForwardController {

    /**
     * 프론트엔드 라우터(App.jsx)에서 정의된 모든 메인 경로들.
     * 해당 주소로 직접 접근하거나 새로고침 시 화면을 정상적으로 띄우도록 index.html을 반환합니다.
     */
    @GetMapping({
        "/login",
        "/signup",
        "/mypage",
        "/find-account",
        "/booking",
        "/seat-selection",
    })
    public String forward() {
        // static 폴더 안의 index.html로 포워딩
        return "forward:/index.html";
    }
}
