package com.mslipper.mailmop;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public class Util {
    public static URL resourceUrl(String name) {
        return Util.class.getResource(name);
    }

    public static File loadResourceAsFile(String name) {
        URL resource = Util.class.getClassLoader().getResource(name);

        if (resource == null) {
            throw new RuntimeException("Could not find resource: " + name);
        }

        return new File(resource.getFile());
    }

    public static <T> List<T> deduplicateBy(List<T> list, Function<T, Object> byProducer) {
        Set<Object> set = new HashSet<>();
        List<T> output = new ArrayList<>();

        for (T item : list) {
            Object by = byProducer.apply(item);

            if (set.contains(by)) {
                continue;
            }

            set.add(by);
            output.add(item);
        }

        return output;
    }
}
