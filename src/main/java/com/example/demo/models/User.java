package com.example.demo.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "users")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class User implements Cloneable, Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "gender", nullable = false)
    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Column(name = "email")
    private String email;

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        User otherUser = (User) obj;
        return Objects.equals(id, otherUser.id) &&
                Objects.equals(firstName, otherUser.firstName) &&
                Objects.equals(lastName, otherUser.lastName) &&
                gender == otherUser.gender;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, firstName, lastName, gender);
    }

    @Override
    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public User clone() {
        return new User(id, firstName, lastName, gender, email);
    }

    public User(String firstName, String lastName, Gender gender) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.gender = gender;
    }

    public enum Gender {
        MALE,
        FEMALE,
        ATTACK_HELICOPTER,
        OTHER
    }
}
