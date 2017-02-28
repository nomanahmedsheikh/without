package org.utd.cs.mln.alchemy.core;

/**
 * Created by Happy on 3/1/17.
 */
public class GroundPredicateSymbol {
    public int id;
    public String symbol;
    public Values values;

    public GroundPredicateSymbol(int id, String symbol, Values values) {
        this.id = id;
        this.symbol = symbol;
        this.values = values;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GroundPredicateSymbol that = (GroundPredicateSymbol) o;

        if (id != that.id) return false;
        if (!symbol.equals(that.symbol)) return false;
        return values.equals(that.values);
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + symbol.hashCode();
        result = 31 * result + values.hashCode();
        return result;
    }
}
