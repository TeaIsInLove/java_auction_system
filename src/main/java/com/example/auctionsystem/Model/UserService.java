package com.example.auctionsystem.Model;

import javax.naming.AuthenticationException;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

public class UserService {
    private static final Map<String, String> users = new HashMap<>();
    private static final Map<String, String> roles = new HashMap<>();

    static {
        users.put("admin", "123");
        users.put("user1", "111");
        users.put("user2", "222");
        users.put("user3", "333");
        users.put("user4", "444");

        roles.put("admin", "ADMIN");
    }


    public static User login(String username, String password) {
        if (users.containsKey(username)
                && users.get(username).equals(password)) {

            String role = roles.getOrDefault(username, "USER");
            return new User(username, password, role);
        }
        return null;
    }

    public static List<User> getAllUsers() {
        List<User> list = new ArrayList<>();

        for (Map.Entry<String, String> entry : users.entrySet()) {
            String username = entry.getKey();
            String password = entry.getValue();
            String role = roles.getOrDefault(username, "USER");
            list.add(new User(entry.getKey(), entry.getValue(), role));
        }

        return list;
    }
    public void register(String username, String password)
            throws AuthenticationException {

        if (username.isEmpty() || password.isEmpty()) {
            throw new AuthenticationException("Không được để trống!");
        }

        if (password.length() < 6) {
            throw new AuthenticationException("Mật khẩu phải >= 6 ký tự!");
        }

        if (users.containsKey(username)) {
            throw new AuthenticationException("Username đã tồn tại!");
        }

        // ✅ QUAN TRỌNG: lưu vào map
        users.put(username, password);
        roles.put(username, "USER");

        System.out.println("User registered: " + username);
    }

}