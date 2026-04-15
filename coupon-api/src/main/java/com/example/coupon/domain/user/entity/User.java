package com.example.coupon.domain.user.entity;

import com.example.coupon.domain.user.exception.InsufficientPointsException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Long point = 0L;

    public User(String email, String name) {
        this.email = email;
        this.name = name;
    }

    public void depositPoints(Long amount) {
        this.point += amount;
    }

    public void usePoints(Long amount) {
        if (this.point < amount) {
            throw new InsufficientPointsException(this.id, amount, this.point);
        }
        this.point -= amount;
    }
}
