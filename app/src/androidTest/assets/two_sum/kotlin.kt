fun twoSum(nums: List<Int>, target: Int): List<Int> {
    val seen = HashMap<Int, Int>()
    nums.forEachIndexed { i, x ->
        seen[target - x]?.let { return listOf(it, i) }
        seen[x] = i
    }
    return emptyList()
}
