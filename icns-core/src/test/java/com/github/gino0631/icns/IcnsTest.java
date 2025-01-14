package com.github.gino0631.icns;

import com.github.gino0631.common.io.IoStreams;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

public class IcnsTest {
    @Test
    public void testParse() throws Exception {
        Set<String> iconTypes = new HashSet<>();
        AtomicInteger imagesLoaded = new AtomicInteger();

        IcnsParser.parse(Files.newInputStream(getResource("/compass.icns")), (osType, type, size, input) -> {
            if (loadImage(osType, type, size, input)) {
                imagesLoaded.incrementAndGet();
            }
            assertTrue(size > 0);
            iconTypes.add(osType);

            return true;
        });

        assertEquals(13, iconTypes.size());
        assertEquals(8, imagesLoaded.get());
    }

    @Test
    public void testLoad() throws Exception {
        try (IcnsIcons icons = IcnsIcons.load(getResource("/compass.icns"))) {
            assertEquals(12, icons.getEntries().size());

            int imagesLoaded = 0;
            for (IcnsIcons.Entry e : icons.getEntries()) {
                try (InputStream is = e.newInputStream()) {
                    if (loadImage(e.getOsType(), e.getType(), e.getSize(), is)) {
                        imagesLoaded++;
                    }
                }
            }

            assertEquals(8, imagesLoaded);
        }
    }

    @Test
    public void testBuild() throws Exception {
        try (IcnsBuilder builder = IcnsBuilder.getInstance()) {
            builder.add(IcnsType.ICNS_16x16_24BIT_IMAGE, Files.newInputStream(getResource("/is32")));
            builder.add(IcnsType.ICNS_16x16_8BIT_MASK, Files.newInputStream(getResource("/s8mk")));
            builder.add(IcnsType.ICNS_32x32_24BIT_IMAGE, Files.newInputStream(getResource("/il32")));
            builder.add(IcnsType.ICNS_32x32_8BIT_MASK, Files.newInputStream(getResource("/l8mk")));
            builder.add(IcnsType.ICNS_128x128_JPEG_PNG_IMAGE, Files.newInputStream(getResource("/ic07_128x128.png")));
            builder.add(IcnsType.ICNS_256x256_JPEG_PNG_IMAGE, Files.newInputStream(getResource("/ic08_256x256.png")));
            builder.add(IcnsType.ICNS_512x512_JPEG_PNG_IMAGE, Files.newInputStream(getResource("/ic09_512x512.png")));
            builder.add(IcnsType.ICNS_16x16_2X_JPEG_PNG_IMAGE, Files.newInputStream(getResource("/ic11_16x16@2x.png")));
            builder.add(IcnsType.ICNS_32x32_2X_JPEG_PNG_IMAGE, Files.newInputStream(getResource("/ic12_32x32@2x.png")));
            builder.add(IcnsType.ICNS_128x128_2X_JPEG_PNG_IMAGE, Files.newInputStream(getResource("/ic13_128x128@2x.png")));
            builder.add(IcnsType.ICNS_256x256_2X_JPEG_PNG_IMAGE, Files.newInputStream(getResource("/ic14_256x256@2x.png")));
            builder.add(IcnsType.ICNS_1024x1024_2X_JPEG_PNG_IMAGE, Files.newInputStream(getResource("/ic10_1024x1024.png")));

            try (IcnsIcons builtIcons = builder.build()) {
                assertEquals(12, builtIcons.getEntries().size());

                Path output = getResource("/").resolve("generated.icns");
                System.out.println(output.toAbsolutePath().toString());
                try (OutputStream os = Files.newOutputStream(output)) {
                    builtIcons.writeTo(os);
                }

                assertArrayEquals(Files.readAllBytes(getResource("/compass.icns")), Files.readAllBytes(output));
            }
        }
    }

    private static boolean loadImage(String osType, IcnsType type, int size, InputStream is) throws IOException {
        AtomicLong counter = new AtomicLong();
        is = IoStreams.count(is, counter::addAndGet);

        try {
            if (!osType.equals(IcnsParser.TOC)) {
                assertNotNull(type);

                if (type.name().contains("PNG")) {
                    BufferedImage image = ImageIO.read(is);
                    assertNotNull(image);
                    assertEquals(type.getWidth(), image.getWidth());
                    assertEquals(type.getHeight(), image.getHeight());

                    return true;
                }
            }

            return false;

        } finally {
            IoStreams.exhaust(is);
            assertEquals(size, counter.intValue());
        }
    }

    private static Path getResource(String name) {
        try {
            return new File(IcnsTest.class.getResource(name).toURI()).toPath();

        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
