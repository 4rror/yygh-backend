package com.atguigu.yygh.order;

import com.atguigu.yygh.order.util.ConstantPropertiesUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;

@Slf4j
@SpringBootTest
public class UtilTest {

    @Test
    public void testCert() {
        log.info("cert: {}", ConstantPropertiesUtils.CERT);
        // File file = new File(ConstantPropertiesUtils.CERT);
        // log.info("file: {}", file.getAbsolutePath());
    }
}
