package com.ssafy.withy.domain.user.repository;

import com.ssafy.withy.domain.user.entity.Friendship;
import com.ssafy.withy.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FriendshipRepository extends JpaRepository<Friendship, Integer> {

    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM Friendship f " +
            "WHERE (f.userA = :user1 AND f.userB = :user2) OR (f.userA = :user2 AND f.userB = :user1)")
    boolean existsFriendship(@Param("user1") User user1, @Param("user2") User user2);

    @Query("SELECT COUNT(f) > 0 FROM Friendship f " +
            "WHERE (f.userA.id = :userId1 AND f.userB.id = :userId2) " +
            "OR (f.userA.id = :userId2 AND f.userB.id = :userId1)")
    boolean existsFriendship(@Param("userId1") Integer userId1, @Param("userId2") Integer userId2);

    List<Friendship> findAllByUserAOrUserB(User userA, User userB);

    @Modifying
    @Query("DELETE FROM Friendship f WHERE (f.userA = :user1 AND f.userB = :user2) OR (f.userA = :user2 AND f.userB = :user1)")
    void deleteFriendship(@Param("user1") User user1, @Param("user2") User user2);

    @Modifying
    @Query("DELETE FROM Friendship f WHERE f.userA = :user OR f.userB = :user")
    void deleteAllByUserAOrUserB(@Param("user") User user);
}
