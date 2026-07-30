package com.example.hotelreservation.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;

@Entity
@Table(name = "hotels")
@SoftDelete(strategy = SoftDeleteType.DELETED, columnName = "deleted")
@Getter
@Setter
@NoArgsConstructor
public class Hotel extends AuditableEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @Column(name = "stars", nullable = false)
    private Integer stars;
}