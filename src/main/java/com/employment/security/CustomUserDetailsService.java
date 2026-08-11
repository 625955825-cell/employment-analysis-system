package com.employment.security;

import com.employment.model.entity.SysRole;
import com.employment.model.entity.SysUser;
import com.employment.model.entity.SysUserRole;
import com.employment.repository.SysRoleRepository;
import com.employment.repository.SysUserRoleRepository;
import com.employment.repository.SysUserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.hibernate.Session;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final SysUserRepository userRepository;
    private final SysUserRoleRepository userRoleRepository;
    private final SysRoleRepository roleRepository;
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Session session = entityManager.unwrap(Session.class);

        List<SysUser> results = session.createNativeQuery("SELECT * FROM sys_user WHERE username = :username", SysUser.class)
                .setParameter("username", username)
                .getResultList();

        SysUser user = results.isEmpty() ? null : results.get(0);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        if ("1".equals(user.getStatus())) {
            throw new UsernameNotFoundException("用户已被禁用: " + username);
        }

        return new User(
                user.getUsername(),
                user.getPassword(),
                user.getStatus().equals("0"),
                true, true, true,
                getAuthorities(user.getId())
        );
    }

    private List<SimpleGrantedAuthority> getAuthorities(Long userId) {
        List<SysUserRole> userRoles = userRoleRepository.findByUserId(userId);
        if (userRoles.isEmpty()) {
            return new ArrayList<>();
        }
        return userRoles.stream()
                .map(ur -> roleRepository.findById(ur.getRoleId()))
                .filter(Optional -> Optional.isPresent())
                .map(Optional -> Optional.get())
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getRoleKey()))
                .collect(Collectors.toList());
    }
}
