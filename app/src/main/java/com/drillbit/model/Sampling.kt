package com.drillbit.model

import kotlin.random.Random

/**
 * Efraimidis-Spirakis 无放回加权抽样（spec 4.3.1）：
 * 每题生成 key = ln(u)/w（u 为 (0,1) 均匀随机数，w 为题目权重），取 key 最大的 N 题，
 * 数学上等价于按权重比例的无放回抽样；key 降序天然作为随机出题顺序。
 */
fun <T> weightedSample(candidates: List<T>, weightOf: (T) -> Int, count: Int): List<T> {
    if (candidates.isEmpty() || count <= 0) return emptyList()
    val n = count.coerceAtMost(candidates.size)
    return candidates
        .asSequence()
        .map { item ->
            val u = Random.nextDouble(Double.MIN_VALUE, 1.0)
            item to kotlin.math.ln(u) / weightOf(item).coerceAtLeast(1)
        }
        .sortedByDescending { it.second }
        .take(n)
        .map { it.first }
        .toList()
}
