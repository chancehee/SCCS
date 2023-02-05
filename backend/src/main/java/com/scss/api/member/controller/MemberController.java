package com.scss.api.member.controller;

import com.scss.api.member.dto.MemberDto;
import com.scss.api.member.dto.UniqueDto;
import com.scss.api.member.service.JWTService;
import com.scss.api.member.service.MemberService;
import com.scss.api.member.util.CookieService;
import com.scss.api.member.util.EmailService;
import com.scss.api.member.util.EncryptService;
import com.scss.api.member.util.RedisService;
import java.lang.reflect.Member;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.sun.org.apache.xpath.internal.operations.Bool;
import io.jsonwebtoken.Claims;
import javax.mail.MessagingException;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping( "/api")
@Api(tags = "회원 컨트롤러 API")
@RequiredArgsConstructor
public class MemberController {

    private static final Logger     logger  = LoggerFactory.getLogger(MemberController.class);
    private static final String     SUCCESS = "success";
    private static final String     FAIL    = "fail";
    private static final int        MINUTE  = 1000 * 60; // 1분
    private static final int        HOUR    = MINUTE * 60; // 1시간
    private static final int        DAY     = HOUR * 24; // 24시간
    private static final int        WEEK    = DAY * 7; // 1주일
    private static final String HEADER_AUTH = "authorization";

    private final MemberService     memberService;
    private final JWTService        jwtService;
    private final EncryptService    encryptService;
    private final EmailService      emailService;
    private final RedisService      redisService;
    private final CookieService     cookieService;

    /** 회원 가입 **/
    @PostMapping("/member")
    public ResponseEntity<?> signUp(@RequestBody MemberDto memberDto)
            throws NoSuchAlgorithmException {
        Map<String, String> resultMap = new HashMap<>();

        String password = memberDto.getPassword(); // 암호화 전 비밀번호
        String salt = encryptService.newSalt(); // 암호용 난수
        String hex = encryptService.encryptPassword(password, salt); // 암호화 후 비밀번호

        logger.debug("[signUp]암호화 후 비밀번호 : {}", hex);
        memberDto.setPassword(hex); // 설정된 DTO가 DB에 반영될 예정
        memberDto.setSalt(salt); // salt 값을 기록. 로그인 처리시 비밀번호 검증시 사용

        if (memberService.signUp(memberDto).equals(SUCCESS)) {
            resultMap.put("message", "회원 가입 성공");
            return new ResponseEntity<>(resultMap, HttpStatus.OK); // 200
        } else {
            resultMap.put("message", "회원 가입 실패");
            return new ResponseEntity<>(resultMap, HttpStatus.UNAUTHORIZED); // 401
        }
    }

    /** 로그인 : 아이디, 비밀번호 일치시 토큰 생성 **/
    @PostMapping("/member/login")
    public ResponseEntity<?> logIn(@RequestBody Map<String, String> paramMap) {
        Map<String, String> resultmap = new HashMap<>(); // 결과를 담는 자료구조

        try {
            MemberDto memberDto = memberService.memberInfo(paramMap.get("id")); // memberDto를 DB에서 조회

            String password = paramMap.get("password"); // 암호화 전 비밀번호
            String salt = memberDto.getSalt(); // DTO에 저장된 salt 값 불러오기
            String hex = encryptService.encryptPassword(password, salt); // 암호화 후 비밀번호

            logger.debug("[logIn]로그인 시도 비번 : {}", hex);
            logger.debug("[logIn]기존   비밀번호 : {}", memberDto.getPassword());

            if (hex.equals(memberDto.getPassword())) { // DB에 저장되어있는 비밀번호와 새롭게 들어온 비밀번호와 같은지 비교
                logger.debug("[logIn]로그인 성공");
                String accessToken = jwtService.createToken(paramMap.get("id"), "accessToken",
                        (MINUTE * 30)); // 30분
                String refreshToken = jwtService.createToken(paramMap.get("id"), "refreshToken",
                        (WEEK)); // 1주일
                resultmap.put("accessToken", accessToken);
                resultmap.put("refreshToken", refreshToken);

//                Cookie accessTokenCookie  = cookieService.createCookie("accessToken", accessToken);
//                Cookie refreshTokenCookie = cookieService.createCookie("refreshToken", refreshToken);

                // Redis에 저장 (key: refreshtoken값, value: 회원 아이디)
                try {
                    redisService.setRefreshTokenWithRedis(refreshToken, memberDto.getId()); // vDeXz2onv3wFn : ssafy
                    logger.debug("[logIn]Redis에 리프레시토큰 저장완료");
                } catch (Exception e) {
                    logger.error(e.getMessage());
                    return new ResponseEntity<>(resultmap, HttpStatus.BAD_GATEWAY);
                }
                logger.debug("accessToken : {}", accessToken);
                logger.debug("refreshToken : {}", refreshToken);

//                response.addCookie(accessTokenCookie);
//                response.addCookie(refreshTokenCookie);

                return new ResponseEntity<>(resultmap, HttpStatus.OK); // 200
            } else {
                logger.debug("[logIn]비밀번호 불일치!!!");
                resultmap.put("message", "아이디나 비밀번호가 잘못되었습니다.");
                return new ResponseEntity<>(resultmap, HttpStatus.UNAUTHORIZED); // 401
            }
        } catch (Exception e) {
            logger.debug("[logIn]아이디가 존재하지 않습니다.");
            resultmap.put("message", "아이디나 비밀번호가 잘못되었습니다.");
            return new ResponseEntity<>(resultmap, HttpStatus.UNAUTHORIZED); // 401
        }
    }

    /** 회원 정보 **/ /** Auth **/
    @GetMapping("/member/{id}")
    public ResponseEntity<?> memberInfo(@PathVariable("id") String pathId, HttpServletRequest request) {
        Map<String, Object> resultMap = new HashMap<>();

        final String token = request.getHeader(HEADER_AUTH).substring("Bearer ".length());
        System.out.println(jwtService.getToken(token).getId());
        System.out.println(jwtService.getToken(token));
        System.out.println(jwtService.getToken(token).get("id"));

        String id = (String) jwtService.getToken(token).get("id");

        logger.debug("회원정보 조회!!!!");
        MemberDto memberDto = memberService.memberInfo(id);

        if (memberDto != null) {
            resultMap.put("data", memberDto);
            return new ResponseEntity<>(resultMap, HttpStatus.OK); // 200
        } else {
            return new ResponseEntity<Void>(HttpStatus.NO_CONTENT); // 204
        }
    }

    /** 아이디 찾기 : 이름과 이메일로 아이디 찾기 **/
    @PostMapping("/member/id")
    public ResponseEntity<?> findId(@RequestBody HashMap<String, String> map) {
        String id = memberService.findId(map); // 이름과 이메일로 아이디 찾기

        if (id != null) {
            return new ResponseEntity<>(id, HttpStatus.OK); // 200
        } else {
            return new ResponseEntity<Void>(HttpStatus.NO_CONTENT); // 204
        }
    }

    /** 회원 정보 수정 **/ /** Auth **/
    @PatchMapping("/member")
    public ResponseEntity<?> modify(@RequestBody HashMap<String, String> param, @CookieValue String accessToken, @CookieValue String refreshToken) {
        Map<String, String> resultMap = new HashMap<>();

        Claims claims = jwtService.getToken(accessToken);   // accessToken에서 Claims 파싱
        String id = (String) claims.get("id");              // accessToken에서 회원 id 파싱

        MemberDto memberDto = memberService.memberInfo(id);

        logger.debug("[modify]수정 전 : {}", memberDto);
        if (param.get("nickname") != null) memberDto.setNickname(param.get("nickname")); // null 이 넘어오면 수정 x (기존 값 유지)
        if (param.get("email") != null) memberDto.setEmail(param.get("email"));
        if (param.get("profile_image") != null) memberDto.setProfileImage(param.get("profile_image"));
        logger.debug("[modify]수정 후 : {}", memberDto);

        if (memberService.modify(memberDto).equals(SUCCESS)) {
            resultMap.put("message", "성공");
            return new ResponseEntity<>(resultMap, HttpStatus.OK); // 200
        } else {
            resultMap.put("message", "권한이 없습니다");
            return new ResponseEntity<>(resultMap, HttpStatus.UNAUTHORIZED); // 401
        }
    }

    /** 비밀번호 수정 **/ /** Auth **/
    @PatchMapping("/member/password")
    public ResponseEntity<?> modifyPassword(@RequestBody HashMap<String, String> param, @CookieValue String accessToken, @CookieValue String refreshToken) {
        String newPassword = param.get("new_password"); // 클라이언트에서 넘어온 변경하고자 하는 비밀번호
        logger.debug("변경하고자 하는 비밀번호 : {}", newPassword);

        Map<String, String> resultMap = new HashMap<>(); // 결과를 담을 자료구조

        Claims claims = jwtService.getToken(accessToken);
        String id = null;
        if (claims != null) id = (String) claims.get("id"); // accessToken에서 회원 id 파싱

        MemberDto memberDto = memberService.memberInfo(id); // DB에서 회원 정보 조회
        memberDto.setPassword(newPassword);

        if (memberService.modifyPassword(memberDto).equals(SUCCESS)) {
            resultMap.put("message", "성공");
            return new ResponseEntity<>(resultMap, HttpStatus.OK); // 200
        } else {
            resultMap.put("message", "실패");
            return new ResponseEntity<>(resultMap, HttpStatus.UNAUTHORIZED); // 401
        }
    }


    /** 아이디, 이메일, 닉네임 중복 검사 **/
    @GetMapping("/unique/{type}/{param}")
    public ResponseEntity<?> duplicateParam(@PathVariable String type, @PathVariable String param) {
        UniqueDto uniqueDto = new UniqueDto(); // 중복여부를 관리하는 DTO
        uniqueDto.setType(type);
        uniqueDto.setParam(param);

        String result = memberService.uniqueParam(uniqueDto); // DB에서 해당 param이 있는지 조회 (값이 있다면 중복임)

        Map<String, Object> resultMap = new HashMap<>(); // 결과를 담는 자료구조

        if (result == null) { // 중복이 존재하지 않는 경우 unique = true
            resultMap.put("unique", true);
            resultMap.put("message", "사용 가능한 " + uniqueDto.getType() + " 입니다");
            return new ResponseEntity<Map<String, Object>>(resultMap, HttpStatus.OK);
        } else { // 중복인 경우 unique = false
            resultMap.put("unique", false);
            resultMap.put("message", "이미 존재하는 " + uniqueDto.getType() + " 입니다");
            return new ResponseEntity<Map<String, Object>>(resultMap, HttpStatus.OK);
        }
    }

    /** 비밀번호 초기화 **/
    @ApiOperation(value = "비밀번호 초기화 with 구글메일", notes = "이메일과 아이디를 입력한다. 그리고 이메일 전송 성공여부에 따라 '성공' 또는 '실패' 문자열을 반환한다.", response = String.class)
    @PostMapping("/member/password")
    public ResponseEntity<?> initPassword(@RequestBody @ApiParam(value = "아이디와 이메일", required = true) Map<String, String> paramMap)
            throws MessagingException {

        logger.debug("paramMap : {}", paramMap);

        String mail = paramMap.get("email"); // 받는 사람 주소
        String id = paramMap.get("id"); // 회원 아이디

        if (emailService.sendEmail(id, mail)){
            return new ResponseEntity<String>("성공", HttpStatus.OK);
        }
        return new ResponseEntity<>("실패", HttpStatus.OK);
    }

    /** access-token 재발급 **/
    // Todo : access , refresh 모두 받아서 refresh 토큰 값으로 id 값을 value 로 redis에 저장
    @GetMapping("/member/refreshToken")
    public ResponseEntity<?> refreshToken(/**@CookieValue String accessToken, @CookieValue String refreshToken **/ HttpServletRequest request ) {
        Map<String, Object> resultMap = new HashMap<>();
        HttpStatus status = HttpStatus.OK;

        final String refreshToken = request.getHeader(HEADER_AUTH).substring("Bearer ".length());

        if (jwtService.checkToken(refreshToken)) {
            Claims claims = jwtService.getToken(refreshToken);
            String id = (String) claims.get("id");
            logger.debug("토큰 인증 성공! 회원 id : {}", id);
            redisService.showAllKeys();
            logger.debug("redis에 저장된 토큰 값 : {}", redisService.getRefreshTokenWithRedis(refreshToken));
            if (id.equals(redisService.getRefreshTokenWithRedis(refreshToken))) {
                String newAccessToken = jwtService.createToken(id, "accessToken", 30 * MINUTE);
                resultMap.put("accessToken", newAccessToken);
                resultMap.put("message", SUCCESS);
                status = HttpStatus.OK;
                logger.debug("리프레쉬 토큰 인증 성공 !! ");
            }
        } else {
            logger.debug("리프레쉬 토큰 사용 불가");
            resultMap.put("message", "로그인 페이지로 이동하세요");
            status = HttpStatus.UNAUTHORIZED;
        }
        return new ResponseEntity<Map<String, Object>>(resultMap, status);
    }

    /** 레디스 전체 키 개수 조회 **/
    @GetMapping("/redisKeys")
    public ResponseEntity<?> showRedisKeys() {
        int result = redisService.showAllKeys();
        return new ResponseEntity<String>("모든 키 조회 성공: " + result + "개",HttpStatus.OK);
    }

    /** 레디스 전체 키-밸류 삭제 **/
    @DeleteMapping("/redisKeys")
    public ResponseEntity<?> deleteKeys() {
        redisService.deleteAllKeys();
        return new ResponseEntity<>("모든 키 삭제 성공", HttpStatus.OK);
    }

}
