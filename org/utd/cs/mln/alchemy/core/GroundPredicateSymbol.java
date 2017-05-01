package org.utd.cs.mln.alchemy.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Happy on 3/1/17.
 */
public class GroundPredicateSymbol {
    public int id;
    public String symbol;
    public Values values;
    public PredicateSymbol.WorldState world;
    public List<String> variable_types = new ArrayList<>();

    public GroundPredicateSymbol(int id, String symbol, Values values, PredicateSymbol.WorldState world, List<String> var_types) {
        this.id = id;
        this.symbol = symbol;
        this.values = values;
        this.world = world;
        this.variable_types = var_types;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GroundPredicateSymbol that = (GroundPredicateSymbol) o;

        if (id != that.id) return false;
        if (symbol != null ? !symbol.equals(that.symbol) : that.symbol != null) return false;
        if (values != null ? !values.equals(that.values) : that.values != null) return false;
        if (world != that.world) return false;
        return variable_types != null ? variable_types.equals(that.variable_types) : that.variable_types == null;

    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (symbol != null ? symbol.hashCode() : 0);
        result = 31 * result + (values != null ? values.hashCode() : 0);
        result = 31 * result + (world != null ? world.hashCode() : 0);
        result = 31 * result + (variable_types != null ? variable_types.hashCode() : 0);
        return result;
    }
}
