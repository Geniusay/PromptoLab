package io.github.timemachinelab;

import org.jasypt.encryption.StringEncryptor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.web.WebAppConfiguration;

@SpringBootTest
public class EncodingUtil {

    @Autowired
    StringEncryptor encryptor;

    @Test
    public void getPass() {
        String sql = encryptor.encrypt("jdbc:postgresql://117.72.211.46:5432/promptolab");
        String user = encryptor.encrypt("root");
        String pwd = encryptor.encrypt("tmlis666");

        System.out.println("sql Encrypted: " + sql);
        System.out.println("Password Encrypted: " + user);
        System.out.println("Password Encrypted: " + pwd);
    }
}
