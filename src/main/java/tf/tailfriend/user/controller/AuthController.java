package tf.tailfriend.user.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tf.tailfriend.global.config.JwtTokenProvider;
import tf.tailfriend.global.config.UserPrincipal;
import tf.tailfriend.user.entity.User;
import tf.tailfriend.user.entity.dto.LoginRequestDto;
import tf.tailfriend.user.entity.dto.UserRegisterDto;
import tf.tailfriend.user.service.AuthService;
import tf.tailfriend.user.service.UserService;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);


    @PostMapping("/api/auth/register")
    public ResponseEntity<?> register(@RequestBody UserRegisterDto dto, HttpServletResponse response, HttpServletRequest request) {
        logger.info("🔥 register() called!");
        logger.debug("📦 DTO received: {}", dto);

        String domain = request.getServerName();
        System.out.println("/api/auth/register : "+domain);

        // 유저 등록
        User savedUser = userService.registerUser(dto); // 반환값 Users로 변경

        boolean isNewUser = savedUser == null;

        String token = jwtTokenProvider.createToken(
                savedUser.getId(),
                savedUser.getSnsAccountId(),
                savedUser.getSnsType().getId(),
                isNewUser
        );
        logger.debug("🔐 Generated token: {}", token);

        response.addHeader("Set-Cookie", createJwtCookie(token,domain).toString());
        response.addHeader("Set-Cookie", clearCookie("signupInfo",domain).toString());

        return ResponseEntity.ok(Map.of("message", "회원가입 및 로그인 성공"));
    }


    @PostMapping("/api/auth/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDto dto, HttpServletResponse response, HttpServletRequest request) {

        String domain = request.getServerName();
        System.out.println("/api/auth/login : "+domain);
        logger.debug("📥 로그인 요청: {}", dto);

        String token = authService.login(dto);
        logger.debug("🔐 JWT 발급: {}", token);

        response.addHeader("Set-Cookie", createJwtCookie(token,domain).toString());

        return ResponseEntity.ok(Map.of("message", "로그인 성공"));
    }


    @GetMapping("/api/auth/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal UserPrincipal user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }

        return ResponseEntity.ok(buildUserInfo(user));
    }

    @PostMapping("/api/auth/logout")
    public ResponseEntity<?> logout(HttpServletResponse response, HttpServletRequest request) {
        String domain = request.getServerName();
        System.out.println("/api/auth/logout : "+domain);
        response.addHeader("Set-Cookie", clearCookie("accessToken",domain).toString());
        return ResponseEntity.ok(Map.of("message", "로그아웃 완료"));
    }


    @GetMapping("/api/auth/check")
    public ResponseEntity<?> checkLogin(@AuthenticationPrincipal UserPrincipal user) {
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("loggedIn", false));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("isNewUser", user.getIsNewUser());

        if (user.getIsNewUser()) {
            response.put("snsAccountId", user.getSnsAccountId());
            response.put("snsTypeId", user.getSnsTypeId());
        }

        return ResponseEntity.ok(response);
    }



    // 🔧 JWT 쿠키 생성
    private ResponseCookie createJwtCookie(String token, String domain) {
        System.out.println("create Cookie : " +domain);
        boolean isLocal = domain.contains("localhost") || domain.contains("127.0.0.1");

        return ResponseCookie.from("accessToken", token)
                .httpOnly(true)
                .secure(!isLocal) // 배포 환경에서는 true
                .path("/")
                .maxAge(Duration.ofDays(1))
                .sameSite(isLocal ? "Lax" : "None")
                .build();
    }

    // 🔧 쿠키 삭제 (0초로 만료)
    private ResponseCookie clearCookie(String name, String domain) {
        System.out.println("delete Cookie : " +domain);
        boolean isLocal = domain.contains("localhost") || domain.contains("127.0.0.1");

        return ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(!isLocal)
                .path("/")
                .maxAge(0)
                .sameSite(isLocal ? "Lax" : "None")
                .build();
    }

    // 🔧 사용자 정보 응답 구조화
    private Map<String, Object> buildUserInfo(UserPrincipal user) {
        Map<String, Object> info = new HashMap<>();
        info.put("userId", user.getUserId());
        info.put("snsAccountId", user.getSnsAccountId());
        info.put("snsTypeId", user.getSnsTypeId());
        return info;
    }



}


