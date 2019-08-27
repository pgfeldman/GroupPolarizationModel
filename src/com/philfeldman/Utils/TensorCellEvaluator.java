package com.philfeldman.Utils;

import com.philfeldman.utils.math.LabeledTensor;

public interface TensorCellEvaluator<T> {
    public T evaluate(int[] indicies, LabeledTensor lt);
}
