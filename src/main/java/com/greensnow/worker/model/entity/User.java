package com.greensnow.worker.model.entity;

import com.greensnow.worker.model.enums.UserType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    private String email;

    private String phoneNumber;

    private String passwordHash;

    @Enumerated(EnumType.STRING)
    private UserType userType;

    private String firstName;

    private String lastName;

    private String profileImageUrl;

    private boolean emailVerified;

    private boolean phoneVerified;

    private boolean active;

    private LocalDateTime lastLoginAt;

}
