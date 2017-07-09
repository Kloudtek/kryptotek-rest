package com.kloudtek.kryptotek.rest;

import com.kloudtek.kryptotek.rest.server.TestHelper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by yannick on 6/24/17.
 */
@Configuration
@ComponentScan
@EnableWebMvc
public class Config {
    @Bean
    public TestController testController() {
        return new TestController();
    }

    @Bean
    public UserDetailsService userDetailsManager() {
        final DefaultSigningUserDetails testUser = new DefaultSigningUserDetails("user", TestHelper.HMAC_KEY, TestHelper.HMAC_KEY,
                new SimpleGrantedAuthority("ROLE_USER"));
        return new UserDetailsService() {
            @Override
            public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
                if( username.equals("user") ) {
                    return testUser;
                } else {
                    throw new UsernameNotFoundException(username+ " not found");
                }
            }
        };
    }

    @Bean
    public SecConfig secConfig(UserDetailsService userDetailsService) {
        return new SecConfig(userDetailsService);
    }
}
