package org.utd.cs.mln.alchemy.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Happy on 2/28/17.
 */
public class Values {
    public String name;
    public List<Integer> values = new ArrayList<>();

    public Values(String name, List<Integer> values) {
        this.name = name;
        this.values = values;
    }

    @Override
    public String toString() {
        return "Values{" +
                "name='" + name + '\'' +
                ", values=" + values +
                '}';
    }
}
