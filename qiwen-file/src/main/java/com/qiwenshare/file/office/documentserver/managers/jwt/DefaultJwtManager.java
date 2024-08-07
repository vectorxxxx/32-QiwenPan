/**
 * (c) Copyright Ascensio System SIA 2021
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.qiwenshare.file.office.documentserver.managers.jwt;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.primeframework.jwt.Signer;
import org.primeframework.jwt.Verifier;
import org.primeframework.jwt.domain.JWT;
import org.primeframework.jwt.hmac.HMACSigner;
import org.primeframework.jwt.hmac.HMACVerifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Component
public class DefaultJwtManager implements JwtManager
{
    @Value("${files.docservice.secret}")
    private String tokenSecret;
    @Autowired
    private ObjectMapper objectMapper;
    //    @Autowired
    //    private JSONParser parser;

    // create document token
    @Override
    public String createToken(Map<String, Object> payloadClaims) {
        try {
            // build a HMAC signer using a SHA-256 hash
            Signer signer = HMACSigner.newSHA256Signer(tokenSecret);
            JWT jwt = new JWT();
            for (String key : payloadClaims.keySet()) {  // run through all the keys from the payload
                jwt.addClaim(key, payloadClaims.get(key));  // and write each claim to the jwt
            }
            return JWT
                    .getEncoder()
                    .encode(jwt, signer);  // sign and encode the JWT to a JSON string representation
        }
        catch (Exception e) {
            return "";
        }
    }

    // check if the token is enabled
    @Override
    public boolean tokenEnabled() {
        return StringUtils.isNotEmpty(tokenSecret);
    }

    // read document token
    @Override
    public JWT readToken(String token) {
        try {
            // build a HMAC verifier using the token secret
            Verifier verifier = HMACVerifier.newVerifier(tokenSecret);
            return JWT
                    .getDecoder()
                    .decode(token, verifier);  // verify and decode the encoded string JWT to a rich object
        }
        catch (Exception exception) {
            return null;
        }
    }

    // parse the body
    @Override
    public JSONObject parseBody(String payload, String header) {
        JSONObject body;
        try {
            body = JSON.parseObject(payload);  // get body parameters by parsing the payload
        }
        catch (Exception ex) {
            throw new RuntimeException("{\"error\":1,\"message\":\"JSON Parsing error\"}");
        }
        if (tokenEnabled()) {  // check if the token is enabled
            String token = (String) body.get("token");  // get token from the body
            if (StringUtils.isEmpty(token)) {  // if token is empty
                if (!StringUtils.isBlank(header)) {  // and the header is defined
                    token = header.startsWith("Bearer ") ?
                            header.substring(7) :
                            header;  // get token from the header (it is placed after the Bearer prefix if it exists)
                }
            }
            if (StringUtils.isBlank(token)) {
                throw new RuntimeException("{\"error\":1,\"message\":\"JWT expected\"}");
            }

            JWT jwt = readToken(token);  // read token
            if (Objects.isNull(jwt)) {
                throw new RuntimeException("{\"error\":1,\"message\":\"JWT validation failed\"}");
            }
            LinkedHashMap<String, Object> claims = null;
            if (jwt.getObject("payload") != null) {  // get payload from the token and check if it is not empty
                try {
                    @SuppressWarnings("unchecked")
                    LinkedHashMap<String, Object> jwtPayload = (LinkedHashMap<String, Object>) jwt.getObject("payload");

                    claims = jwtPayload;
                }
                catch (Exception ex) {
                    throw new RuntimeException("{\"error\":1,\"message\":\"Wrong payload\"}");
                }
            }
            try {
                body = JSON.parseObject(JSON.toJSONString(claims));
            }
            catch (Exception ex) {
                throw new RuntimeException("{\"error\":1,\"message\":\"Parsing error\"}");
            }
        }

        return body;
    }
}
