package com.music.music.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.music.music.user.entity.UserSocialAccount;

@Repository
public interface UserSocialAccountRepository extends JpaRepository<UserSocialAccount, Long> {

  Optional<UserSocialAccount> findByProviderAndProviderId(String provider, String providerId);
}
