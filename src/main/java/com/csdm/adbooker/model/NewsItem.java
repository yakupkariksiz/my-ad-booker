package com.csdm.adbooker.model;

import lombok.*;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Builder
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Setter
@Getter
@NoArgsConstructor
@Entity
public class NewsItem implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column
    private String guid;

    @Column
    private String title;

    @Column(length = 10000)
    private String description;

    @Column
    private LocalDateTime publishedDate;

    @Column
    private String imageUrl;

}
