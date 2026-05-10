package com.descomplica.frameblog;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class TestarSenha {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        String senhaQueQueroUsar = "123456"; // ← a senha que você vai usar no Postman

        System.out.println(encoder.encode(senhaQueQueroUsar));
    }
}
