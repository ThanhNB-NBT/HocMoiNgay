using System.Collections.Generic;

public class Solution {
    public static List<int> TwoSum(List<int> nums, int target) {
        var seen = new Dictionary<int, int>();
        for (int i = 0; i < nums.Count; i++) {
            if (seen.TryGetValue(target - nums[i], out var j)) return new List<int> { j, i };
            seen[nums[i]] = i;
        }
        return new List<int>();
    }
}
