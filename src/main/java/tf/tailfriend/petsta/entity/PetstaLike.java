package tf.tailfriend.petsta.entity;

import jakarta.persistence.*;
import lombok.*;
import tf.tailfriend.user.entity.User;

import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "petsta_likes")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PetstaLike {

    @EmbeddedId
    private PetStaLikeId id;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @MapsId("postId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "petsta_post_id")
    private PetstaPost petstaPost;

    @Embeddable
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PetStaLikeId implements Serializable {

        @Column(name = "user_id")
        private Integer userId;

        @Column(name = "petsta_id")
        private Integer postId;

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            PetStaLikeId that = (PetStaLikeId) obj;
            return userId.equals(that.userId) && postId.equals(that.postId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, postId);
        }
    }

    public static PetstaLike of(User user, PetstaPost petstaPost) {
        PetStaLikeId id = PetStaLikeId.builder()
                .userId(user.getId())
                .postId(petstaPost.getId())
                .build();

        return PetstaLike.builder()
                .id(id)
                .user(user)
                .petstaPost(petstaPost)
                .build();
    }
}
