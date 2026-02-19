package com.ssafy.withy.domain.party.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.ssafy.withy.domain.content.entity.MediaType;
import com.ssafy.withy.domain.party.entity.Party;
import com.ssafy.withy.domain.party.entity.PlatformType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.ssafy.withy.domain.party.entity.QParty.party;
import static com.ssafy.withy.domain.content.entity.QContent.content;
import static com.ssafy.withy.domain.content.entity.QContentGenre.contentGenre;
import static com.ssafy.withy.domain.content.entity.QGenre.genre;

@Repository
@RequiredArgsConstructor
public class PartyRepositoryImpl implements PartyRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Party> searchParties(PlatformType platform, String category, Boolean isActive, Pageable pageable) {
        BooleanBuilder builder = new BooleanBuilder();

        builder.and(party.isDeleted.isFalse());

        // 기본 조건
        builder.and(party.platform.eq(platform));

        // 활성화 상태 필터링 (null이면 전체)
        if (isActive != null) {
            builder.and(party.isActive.eq(isActive));
        }

        // 카테고리 필터링
        if (category != null && !category.isEmpty()) {
            applyCategoryFilter(builder, platform, category);
        }

        // 기본 쿼리
        JPAQuery<Party> query = queryFactory
                .selectFrom(party)
                .leftJoin(party.content, content).fetchJoin();

        // 조인 적용 (where보다 먼저!)
        if (category != null && !category.isEmpty()) {
            query.leftJoin(content.contentGenres, contentGenre)
                    .leftJoin(contentGenre.genre, genre);
        }

        // 조건 적용
        query.where(builder);

        // fetch 결과
        // fetch 결과
        List<Party> fetch = query
                .distinct()
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(getOrderSpecifiers(pageable))
                .fetch();

        // count 쿼리
        JPAQuery<Long> countQuery = queryFactory
                .select(party.countDistinct())
                .from(party)
                .leftJoin(party.content, content);

        if (category != null && !category.isEmpty()) {
            countQuery.leftJoin(content.contentGenres, contentGenre)
                    .leftJoin(contentGenre.genre, genre);
        }

        countQuery.where(builder);

        return new PageImpl<>(fetch, pageable, countQuery.fetchOne());
    }

    private void applyCategoryFilter(BooleanBuilder builder, PlatformType platform, String category) {
        if (platform == PlatformType.YOUTUBE) {
            try {
                Integer code = Integer.parseInt(category);
                builder.and(genre.type.eq(PlatformType.YOUTUBE).and(genre.code.eq(code)));
            } catch (NumberFormatException e) {
                // 숫자가 아니면 장르 이름으로 검색
                builder.and(genre.type.eq(PlatformType.YOUTUBE).and(genre.name.eq(category)));
            }
        } else if (platform == PlatformType.OTT) {
            if ("KO".equalsIgnoreCase(category)) {
                builder.and(content.mediaType.eq(MediaType.MOVIE));
                builder.and(content.originalLanguage.eq("ko"));
            } else if ("FOREIGN".equalsIgnoreCase(category)) {
                builder.and(content.mediaType.eq(MediaType.MOVIE));
                builder.and(content.originalLanguage.ne("ko"));
            } else {
                try {
                    Integer code = Integer.parseInt(category);
                    builder.and(genre.type.eq(PlatformType.OTT).and(genre.code.eq(code)));
                } catch (NumberFormatException e) {
                    // 숫자가 아니면 장르 이름으로 검색
                    builder.and(genre.type.eq(PlatformType.OTT).and(genre.name.eq(category)));
                }
            }
        }

    }

    private com.querydsl.core.types.OrderSpecifier<?>[] getOrderSpecifiers(Pageable pageable) {
        if (pageable.getSort().isEmpty()) {
            return new com.querydsl.core.types.OrderSpecifier[]{party.scheduledActiveTime.asc()};
        }

        List<com.querydsl.core.types.OrderSpecifier<?>> orders = new java.util.ArrayList<>();
        for (org.springframework.data.domain.Sort.Order order : pageable.getSort()) {
            com.querydsl.core.types.Order direction = order.getDirection().isAscending() ? com.querydsl.core.types.Order.ASC : com.querydsl.core.types.Order.DESC;
            switch (order.getProperty()) {
                case "createdAt":
                    orders.add(new com.querydsl.core.types.OrderSpecifier<>(direction, party.createdAt));
                    break;
                case "scheduledActiveTime":
                    orders.add(new com.querydsl.core.types.OrderSpecifier<>(direction, party.scheduledActiveTime));
                    break;
                case "title":
                    orders.add(new com.querydsl.core.types.OrderSpecifier<>(direction, party.title));
                    break;
                case "currentParticipants":
                    orders.add(new com.querydsl.core.types.OrderSpecifier<>(direction, party.currentParticipants));
                    break;
                case "isActive": // 활성 상태 정렬 지원
                    orders.add(new com.querydsl.core.types.OrderSpecifier<>(direction, party.isActive));
                    break;
                default:
                    // 기본 정렬: 활성화 예정 시간
                    orders.add(new com.querydsl.core.types.OrderSpecifier<>(com.querydsl.core.types.Order.ASC, party.scheduledActiveTime));
                    break;
            }
        }
        return orders.toArray(new com.querydsl.core.types.OrderSpecifier[0]);
    }
}
