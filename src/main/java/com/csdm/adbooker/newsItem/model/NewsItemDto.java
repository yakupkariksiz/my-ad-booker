package com.csdm.adbooker.newsItem.model;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Builder
@Getter
@Setter
@EqualsAndHashCode
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class NewsItemDto implements Serializable {

    private String guid;
    private String title;
    private String description;
    private LocalDateTime publishedDate;
    private String imageUrl;

}
