package com.comp3334_t67.server.repos;

import com.comp3334_t67.server.models.FriendRequest;
import com.comp3334_t67.server.enums.FriendRequestStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface FriendRequestRepository extends JpaRepository<FriendRequest, UUID> {

    public List<FriendRequest> findByReceiverIdAndStatus(UUID receiverId, FriendRequestStatus status);
    public List<FriendRequest> findBySenderIdAndStatus(UUID senderId, FriendRequestStatus status);
    public boolean existsBySenderIdAndReceiverIdAndStatus(UUID senderId, UUID receiverId, FriendRequestStatus status);
}
