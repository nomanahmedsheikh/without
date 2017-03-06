package org.utd.cs.mln.alchemy.core;

import org.utd.cs.gm.core.LogDouble;

import java.util.*;

/**
 * Created by Happy on 3/1/17.
 */
public class GroundClause {
    public List<Integer> groundPredIndices; // Stores indices of groundpredicates coming in this clause. Indices are of MLN's groundPredicates list.
    public Map<Integer, Integer> globalToLocalPredIndex; // Maps index of MLN's groundPredicate to index of clause's groundpredicate
    public List<BitSet> grounPredBitSet;
    public LogDouble weight;
    public int formulaId; // id of groundformula of which this is a part
    public boolean isSatisfied;

    public GroundClause() {
        groundPredIndices = new ArrayList<>();
        globalToLocalPredIndex = new HashMap<>();
        grounPredBitSet = new ArrayList<>();
        weight = LogDouble.ZERO;
        isSatisfied = false;
        formulaId = 0;

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GroundClause that = (GroundClause) o;

        if (formulaId != that.formulaId) return false;
        if (isSatisfied != that.isSatisfied) return false;
        if (!groundPredIndices.equals(that.groundPredIndices)) return false;
        if (!globalToLocalPredIndex.equals(that.globalToLocalPredIndex)) return false;
        if (!grounPredBitSet.equals(that.grounPredBitSet)) return false;
        return weight.equals(that.weight);
    }

    @Override
    public int hashCode() {
        int result = groundPredIndices.hashCode();
        result = 31 * result + globalToLocalPredIndex.hashCode();
        result = 31 * result + grounPredBitSet.hashCode();
        result = 31 * result + weight.hashCode();
        result = 31 * result + formulaId;
        result = 31 * result + (isSatisfied ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "GroundClause{" +
                "groundPredIndices=" + groundPredIndices +
                ", globalToLocalPredIndex=" + globalToLocalPredIndex +
                ", grounPredBitSet=" + grounPredBitSet +
                ", weight=" + weight +
                ", formulaId=" + formulaId +
                ", isSatisfied=" + isSatisfied +
                '}';
    }
}

