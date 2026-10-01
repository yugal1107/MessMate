package com.springboot.MessApplication.MessMate.entities;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.springboot.MessApplication.MessMate.entities.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import org.checkerframework.common.aliasing.qual.Unique;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name="mess_user")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "email_verified")
    private Boolean emailVerified;

    private String password;
    private String name;

    @Enumerated(EnumType.STRING)
    private Role role;

    @OneToOne(fetch = FetchType.LAZY , cascade = CascadeType.ALL) //fetch is eager by default for one to one
    private Subscription subscription;


    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private MealOff mealOff;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL) //fetch type is lazy by default in one to many
    private List<Notification> notifications;

    private String address;
    private String contact;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + this.getRole().toString()));
    }

    @Override
    public String getUsername() {
        return this.email;
    }

    @Override
    public boolean isEnabled() {
        // Existing users with a null value remain enabled during migration.
        return !Boolean.FALSE.equals(emailVerified);
    }
}
