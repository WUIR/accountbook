package com.example.accountbook.model;

public class User {

  public static final String DEFAULT_NICKNAME = "轻账用户";
  public static final String DEFAULT_ROLE_LABEL = "普通用户";

  private long id;
  private String username;
  private String passwordHash;
  private String passwordSalt;
  private String nickname;
  private String avatarPath;
  private String signature;
  private String roleLabel;
  private long createdAt;
  private long updatedAt;
  private long lastLoginAt;

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
  }

  public String getPasswordSalt() {
    return passwordSalt;
  }

  public void setPasswordSalt(String passwordSalt) {
    this.passwordSalt = passwordSalt;
  }

  public String getNickname() {
    return nickname;
  }

  public void setNickname(String nickname) {
    this.nickname = nickname;
  }

  public String getAvatarPath() {
    return avatarPath;
  }

  public void setAvatarPath(String avatarPath) {
    this.avatarPath = avatarPath;
  }

  public String getSignature() {
    return signature;
  }

  public void setSignature(String signature) {
    this.signature = signature;
  }

  public String getRoleLabel() {
    return roleLabel;
  }

  public void setRoleLabel(String roleLabel) {
    this.roleLabel = roleLabel;
  }

  public long getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(long createdAt) {
    this.createdAt = createdAt;
  }

  public long getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(long updatedAt) {
    this.updatedAt = updatedAt;
  }

  public long getLastLoginAt() {
    return lastLoginAt;
  }

  public void setLastLoginAt(long lastLoginAt) {
    this.lastLoginAt = lastLoginAt;
  }
}
