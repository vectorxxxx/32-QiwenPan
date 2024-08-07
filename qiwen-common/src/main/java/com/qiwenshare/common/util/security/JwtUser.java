package com.qiwenshare.common.util.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class JwtUser implements UserDetails
{
    private String userId;
    private String username;

    private String password;

    private Integer available;

    private Collection<? extends GrantedAuthority> authorities;

    /**
     * 帐户是否未过期
     *
     * @return boolean
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * 帐户是否未锁定
     *
     * @return boolean
     */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /**
     * 凭据是否未过期
     *
     * @return boolean
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * 是否启用
     *
     * @return boolean
     */
    @Override
    public boolean isEnabled() {
        return !"0".equals(available);
    }
}
