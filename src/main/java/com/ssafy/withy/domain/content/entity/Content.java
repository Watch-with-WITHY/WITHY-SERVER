package com.ssafy.withy.domain.content.entity;

import com.ssafy.withy.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "contents")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Content extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "external_id", length = 255)
    private String externalId;

    @Column(name = "tmdb_id")
    private Integer tmdbId;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false)
    private MediaType mediaType;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "original_title", length = 255)
    private String originalTitle;

    @Column(name = "overview", columnDefinition = "TEXT")
    private String overview;

    @Column(name = "release_date", length = 20)
    private String releaseDate;

    @Column(name = "poster_path", length = 256)
    private String posterPath;

    @Column(name = "original_language", length = 10)
    private String originalLanguage;

    @Builder.Default
    @OneToMany(mappedBy = "content", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ContentGenre> contentGenres = new ArrayList<>();

    public void linkWithNetflix(String externalId, String originalTitle) {
        this.externalId = externalId;
        this.originalTitle = originalTitle;
    }

    // 언어 정보 갱신용
    public void updateOriginalLanguage(String originalLanguage) {
        this.originalLanguage = originalLanguage;
    }

    // 장르 추가
    public void addGenre(Genre genre) {
        ContentGenre contentGenre = ContentGenre.builder()
                .content(this)
                .genre(genre)
                .build();
        this.contentGenres.add(contentGenre);
    }

    // 장르 초기화 (갱신 시 사용)
    public void clearGenres() {
        this.contentGenres.clear();
    }
}
