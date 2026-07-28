package com.moyeo.development;

enum DevTestAccount {

    USER_ONE("슈퍼토큰유저"),
    USER_TWO("개발 사용자 2");

    private final String nickname;

    DevTestAccount(String nickname) {
        this.nickname = nickname;
    }

    String nickname() {
        return nickname;
    }
}
