package com.example.chartographer.web.controllers;

import com.example.chartographer.config.StartupRunner;
import com.example.chartographer.image.Charta;
import com.example.chartographer.image.ImagePiece;
import com.example.chartographer.image.ImageUtilsService;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;

@RestController
@RequestMapping("/chartas")
public class ChartasController {

    @Autowired
    private Gson gson;

    @Autowired
    private ImageUtilsService imageUtils;

    @PostMapping(value = "/", params = {
            "width",
            "height"
    })
    private ResponseEntity<Integer> createNewCharta(@RequestParam int width, @RequestParam int height) {

        if (!verifyWidthHeightCharta(width, height)) {
            return ResponseEntity.badRequest().build();
        }

        Charta c = new Charta(width, height);
        try (
                Writer writer = Files.newBufferedWriter(StartupRunner.workDirResolve(c.getId() + ".json"))
        ) {
            imageUtils.createEmptyBMPfile(width, height, String.valueOf(c.getId()));
            gson.toJson(c, writer);
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
        return new ResponseEntity<>(c.getId(), HttpStatus.CREATED);
    }

    @PostMapping(value = "/{id}")
    private ResponseEntity<Void> saveFragment(@PathVariable("id") int id,
                                              @RequestParam int x,
                                              @RequestParam int y,
                                              @RequestParam int width,
                                              @RequestParam int height,
                                              HttpServletRequest request) {

        if (!verifyWidthHeightCharta(width, height) || !verifyXY(x, y) || !verifyId(id)) {
            return ResponseEntity.badRequest().build();
        }

        try (
                Reader reader = Files.newBufferedReader(StartupRunner.workDirResolve(id + ".json"))
        ) {
            Charta c = gson.fromJson(reader, Charta.class);
            InputStream stream = request.getInputStream();
            reader.close();

            if (!imageUtils.appendToImage(c, stream, id, x, y, width, height)) {
                return ResponseEntity.badRequest().build();
            }

            Writer writer = Files.newBufferedWriter(StartupRunner.workDirResolve(id + ".json"));
            c.addPiece(new ImagePiece(width, height, x, y));
            gson.toJson(c, writer);
            writer.close();
        } catch (NoSuchFileException fnf) {
            return ResponseEntity.notFound().build();
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping(value = "/{id}", produces = "image/bmp")
    private ResponseEntity<?> getFragment(@PathVariable("id") int id,
                                          @RequestParam int x,
                                          @RequestParam int y,
                                          @RequestParam int width,
                                          @RequestParam int height) {

        if (!verifyWidthHeightPiece(width, height) || !verifyXY(x, y) || !verifyId(id)) {
            return ResponseEntity.badRequest().build();
        }

        try (
                Reader fileReader = Files.newBufferedReader(StartupRunner.workDirResolve(id + ".json"));
                ByteArrayOutputStream out = new ByteArrayOutputStream()
        ) {
            Charta c = gson.fromJson(fileReader, Charta.class);
            if (!c.hasFragmentInArea(x, y, width, height)) {
                return ResponseEntity.badRequest().body("no fragment in given area");
            }
            BufferedImage image = imageUtils.readFragment(c, x, y, width, height);
            ImageIO.write(image, "bmp", out);
            return ResponseEntity.ok().body(out.toByteArray());
        } catch (NoSuchFileException fnf) {
            return ResponseEntity.notFound().build();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ResponseEntity.badRequest().build();
    }

    @DeleteMapping("/{id}")
    private ResponseEntity<Void> deleteCharta(@PathVariable("id") int id) {
        if (!verifyId(id)) {
            return ResponseEntity.badRequest().build();
        }
        if (imageUtils.deleteCharta(id)) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    private boolean verifyId(int id) {
        return id > 0;
    }

    private boolean verifyWidthHeightCharta(int width, int height) {
        return width <= Charta.MAX_WIDTH && width >= Charta.MIN_WIDTH &&
                height <= Charta.MAX_HEIGHT && height >= Charta.MIN_HEIGHT;
    }

    private boolean verifyWidthHeightPiece(int width, int height) {
        return width <= Charta.MAX_WIDTH && width >= Charta.MIN_WIDTH &&
                height <= Charta.MAX_HEIGHT && height >= Charta.MIN_HEIGHT;
    }

    private boolean verifyXY(int x, int y) {
        return x >= 0 && y >= 0;
    }


}