package com.zentide.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CollectionComparator<T> {

    public static class DiffResult<T> {
        public List<T> addList = new ArrayList<>();
        public List<T> updateList = new ArrayList<>();
        public List<T> deletedList = new ArrayList<>();
    }

    public  DiffResult<T> compare(Collection<T> newList,
                                 Collection<T> oldList,
                                 Function<T, String> idExtractor) {
        DiffResult<T> result = new DiffResult<>();
        // 使用Map存储，key为ID，value为对象
        Map<String, T> oldMap = oldList.stream()
                .collect(Collectors.toMap(idExtractor, Function.identity(), (data1, data2) -> data2));
        Map<String, T> newMap = newList.stream()
                .collect(Collectors.toMap(idExtractor, Function.identity(), (data1, data2) -> data2));
        // 找出新增和修改的
        for (Map.Entry<String, T> entry : newMap.entrySet()) {
            String key = entry.getKey();
            T newObj = entry.getValue();
            if (!oldMap.containsKey(key)) {
                result.addList.add(newObj);
            } else {
                result.updateList.add(newObj);
            }
        }
        // 找出删除的
        for (Map.Entry<String, T> entry : oldMap.entrySet()) {
            String key = entry.getKey();
            if (!newMap.containsKey(key)) {
                result.deletedList.add(entry.getValue());
            }
        }
        return result;
    }
}