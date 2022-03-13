package com.example.chartographer;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.chartographer.image.ImageUtilsService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.util.Assert;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@TestMethodOrder(MethodOrderer.DisplayName.class)
class ChartographerApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ImageUtilsService imageUtils;

    @AfterAll
    @BeforeAll
    public static void cleanup() throws IOException {
        Files.deleteIfExists(Path.of("1.bmp"));
        Files.deleteIfExists(Path.of("1.json"));
        Files.deleteIfExists(Path.of("options.json"));
    }

    @Test
    public void test1_createChartasOK() throws Exception {
        for (int i = 1; i <= 10 ; i++) {
            createCharta(3000 + i, 5000 + i)
                    .andExpect(status().isCreated())
                    .andExpect(content().string(i + ""));
        }
    }

    @Test
    public void test2_createChartasBadParameters() throws Exception {
        createCharta(0, 200)
                .andExpect(status().isBadRequest());
        createCharta(200, 0)
                .andExpect(status().isBadRequest());
        createCharta(-100, -100)
                .andExpect(status().isBadRequest());
        createCharta(30000, 100)
                .andExpect(status().isBadRequest());
        createCharta(20001, 50001)
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/chartas/")
                .queryParam("width", "qwerty")
                .queryParam("height", "0000"))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    public void test3_removeChartasOK() throws Exception {
        for (int i = 2; i <= 10; i++) {
            mockMvc.perform(delete("/chartas/" + i))
                    .andExpect(status().isOk());
            getFragment(i, 100, 100, 0, 0)
                    .andExpect(status().isNotFound());
        }
    }

    @Test
    public void test4_removeChartasBadID() throws Exception {
        mockMvc.perform(delete("/chartas/-1"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(delete("/chartas/100"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void test5_saveGetFragmentOK() throws Exception {
        saveAndGetFragmentDefault(1, 1280, 853, 10, 10, "sample_1280x853.bmp");
        saveAndGetFragmentDefault(1, 124, 124, 50, 50, "FLAG_B24.BMP");
        saveAndGetFragmentDefault(1, 1, 1, 2800, 2800, "green_pixel.bmp");
        saveAndGetFragmentDefault(1, 333, 149, 2600, 2600, "non-zero-padding.bmp");
    }

    @Test
    public void test6_saveFragmentBadRequest() throws Exception {
        byte[] img = Files.readAllBytes(Path.of("bmp_samples/" + "sample_1280x853.bmp"));
        saveFragment(1, 30000, 20000, 10, 20, img)
                .andExpect(status().isBadRequest());
    }

    @Test
    public void test7_getNonExistingFragment() throws Exception {
        getFragment(1, 1, 1, 2900, 2900)
                .andExpect(status().isBadRequest());
    }

    @Test
    public void test8_getFragmentBadParams() throws Exception {
        getFragment(1, -10, 100, 4, 4)
                .andExpect(status().isBadRequest());
        getFragment(1, 100000, 100, -98000, 4)
                .andExpect(status().isBadRequest());
        getFragment(1, 40000, 50000, 4, -480000)
                .andExpect(status().isBadRequest());
        getFragment(1, 10, 100, -100, -10)
                .andExpect(status().isBadRequest());
    }

    private void saveAndGetFragmentDefault(int id, int width, int height, int x, int y, String filename) throws Exception {
        byte[] original = Files.readAllBytes(Path.of("bmp_samples/" + filename));
        int originalHeaderSize = imageUtils.validateBmpHeader(original);
        saveFragment(id, width, height, x, y, original)
                .andExpect(status().isOk());
        getFragment(id, width, height, x, y)
                .andExpect(status().isOk())
                .andExpect(result -> {
                    byte[] body = result.getResponse().getContentAsByteArray();
                    int resultHeaderSize = imageUtils.validateBmpHeader(body);
                    Assert.isTrue(Arrays.equals(original,
                            ImageUtilsService.BITMAP_FILE_HEADER_SIZE + originalHeaderSize,
                            original.length - 1,
                            body,
                            ImageUtilsService.BITMAP_FILE_HEADER_SIZE + resultHeaderSize,
                            body.length - 1), "Original image differs from response");
                });
    }

    private ResultActions getFragment(int id, int width, int height, int x, int y) throws Exception {
        return mockMvc.perform(get("/chartas/" + id)
                .queryParam("width", width + "")
                .queryParam("height", height + "")
                .queryParam("x", x + "")
                .queryParam("y", y + ""));
    }

    private ResultActions saveFragment(int id, int width, int height, int x, int y, byte[] imgBytes) throws Exception {
        return mockMvc.perform(post("/chartas/" + id).content(imgBytes)
                .queryParam("width", width + "")
                .queryParam("height", height + "")
                .queryParam("x", x + "")
                .queryParam("y", y + ""));

    }

    private ResultActions createCharta(int width, int height) throws Exception {
        return this.mockMvc.perform(post("/chartas/")
                        .queryParam("width", width + "")
                        .queryParam("height", height + ""))
                .andDo(print());
    }

}
