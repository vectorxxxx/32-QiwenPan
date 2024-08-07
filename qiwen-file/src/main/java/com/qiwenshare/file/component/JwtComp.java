package com.qiwenshare.file.component;

import com.alibaba.fastjson2.JSON;
import com.qiwenshare.common.util.math.CalculatorUtils;
import com.qiwenshare.file.config.jwt.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.DefaultClaims;
import org.apache.commons.codec.binary.Base64;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Date;
import java.util.Map;

@Component
public class JwtComp
{

    @Resource
    JwtProperties jwtProperties;

    // 由字符串生成加密key
    private SecretKey generalKey() {
        // 本地的密码解码
        byte[] encodedKey = Base64.decodeBase64(jwtProperties.getSecret());
        // 根据给定的字节数组使用AES加密算法构造一个密钥
        return new SecretKeySpec(encodedKey, 0, encodedKey.length, "AES");
    }

    // 创建jwt
    public String createJWT(Map<String, Object> param) {
        String subject = JSON.toJSONString(param);
        // 生成JWT的时间
        long nowTime = System.currentTimeMillis();
        Date nowDate = new Date(nowTime);
        // 生成签名的时候使用的秘钥secret，切记这个秘钥不能外露，是你服务端的私钥，在任何场景都不应该流露出去，一旦客户端得知这个secret，那就意味着客户端是可以自我签发jwt的
        SecretKey key = generalKey();
        Double expireTime = CalculatorUtils.conversion(jwtProperties
                .getPayload()
                .getRegisterdClaims()
                .getExp());

        // 为payload添加各种标准声明和私有声明
        Claims defaultClaims = new DefaultClaims()
                // 签发人
                .setIssuer(jwtProperties
                        .getPayload()
                        .getRegisterdClaims()
                        .getIss())
                // 过期时间
                .setExpiration(new Date(System.currentTimeMillis() + expireTime.longValue()))
                // 主题
                .setSubject(subject)
                // 签收人
                .setAudience(jwtProperties
                        .getPayload()
                        .getRegisterdClaims()
                        .getAud());

        return Jwts
                // 表示new一个JwtBuilder，设置jwt的body
                .builder()
                .setClaims(defaultClaims)
                // iat(issuedAt)：jwt的签发时间
                .setIssuedAt(nowDate)
                // 设置签名，使用的是签名算法和签名使用的秘钥
                .signWith(SignatureAlgorithm.forName(jwtProperties
                        .getHeader()
                        .getAlg()), key)
                .compact();
    }

    // 解密jwt
    public Claims parseJWT(String jwt) throws Exception {
        SecretKey key = generalKey(); // 签名秘钥，和生成的签名的秘钥一模一样
        return Jwts.parser() // 得到DefaultJwtParser
                   .setSigningKey(key) // 设置签名的秘钥
                   .parseClaimsJws(jwt)
                   .getBody();
    }

}
