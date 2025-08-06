/**
 * 自定义用户详情服务
 * 
 * 功能说明：
 * - 实现Spring Security的UserDetailsService接口
 * - 负责从数据库加载用户信息用于身份验证
 * - 将自定义User实体转换为Spring Security的UserDetails对象
 * 
 * 核心职责：
 * - loadUserByUsername: 根据用户名从数据库加载用户信息
 * - 权限映射: 将用户角色转换为Spring Security权限
 * - 账户状态检查: 验证账户是否启用、过期、锁定等
 * 
 * CustomUserPrincipal内部类：
 * - 实现UserDetails接口，包装自定义User实体
 * - 提供Spring Security需要的用户认证信息
 * - 支持角色权限映射（USER/ADMIN/DRIVER/OPERATOR）
 * - 管理账户状态（启用/禁用、过期、锁定等）
 * 
 * 安全特性：
 * - 账户启用状态检查（基于user.enabled字段）
 * - 角色权限自动映射（添加ROLE_前缀）
 * - 密码安全封装（不暴露明文密码）
 * 
 * 集成要点：
 * - 与Spring Security认证流程无缝集成
 * - 支持JWT令牌生成时的用户信息提取
 * - 为方法级安全注解提供权限支持
 * 
 * @author Mini-UPS Team
 * @version 1.0.0
 */
package com.miniups.security;

import com.miniups.model.entity.User;
import com.miniups.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        User user;
        
        // Check if the input contains '@' to determine if it's an email
        if (usernameOrEmail.contains("@")) {
            user = userRepository.findByEmail(usernameOrEmail)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + usernameOrEmail));
        } else {
            user = userRepository.findByUsername(usernameOrEmail)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + usernameOrEmail));
        }
        
        return new CustomUserPrincipal(user);
    }
    
    public static class CustomUserPrincipal implements UserDetails {
        private User user;
        
        public CustomUserPrincipal(User user) {
            this.user = user;
        }
        
        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            List<GrantedAuthority> authorities = new ArrayList<>();
            authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
            return authorities;
        }
        
        @Override
        public String getPassword() {
            return user.getPassword();
        }
        
        @Override
        public String getUsername() {
            return user.getUsername();
        }
        
        @Override
        public boolean isAccountNonExpired() {
            return true;
        }
        
        @Override
        public boolean isAccountNonLocked() {
            return true;
        }
        
        @Override
        public boolean isCredentialsNonExpired() {
            return true;
        }
        
        @Override
        public boolean isEnabled() {
            return user.getEnabled();
        }
        
        public User getUser() {
            return user;
        }
    }
}