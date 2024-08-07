package com.qiwenshare.file.util;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author VectorX
 * @version V1.0
 * @description 实体复制实用程序
 * @date 2024-08-06 16:39:56
 */
@Slf4j
public class BeanCopyUtils
{
    /**
     * 复制分页对象
     *
     * @param sourceIPage 源IPage
     * @param targetClass 目标类别
     * @return {@link IPage }<{@link T }>
     */
    public static <S, T> IPage<T> copyPage(IPage<S> sourceIPage, Class<T> targetClass) {
        IPage<T> targetIPage = new Page<>();
        BeanUtils.copyProperties(sourceIPage, targetIPage);
        targetIPage.setRecords(BeanCopyUtils.copyList(sourceIPage.getRecords(), targetClass));
        return targetIPage;
    }

    /**
     * 复制集合对象
     *
     * @param beanList    bean集合
     * @param targetClass 目标类型
     * @return {@link List }<{@link T }>
     */
    public static <S, T> List<T> copyList(List<S> beanList, Class<T> targetClass) {
        return beanList
                .stream()
                .map(s -> copy(s, targetClass))
                .collect(Collectors.toList());
    }

    /**
     * 复制实体对象
     *
     * @param bean        bean
     * @param targetClass 目标类型
     * @return {@link T }
     */
    public static <S, T> T copy(S bean, Class<T> targetClass) {
        try {
            T t = targetClass.newInstance();
            BeanUtils.copyProperties(bean, t);
            return t;
        }
        catch (Exception e) {
            log.error("BeanCopyUtils.copy error: {}", e.getMessage(), e);
        }
        return null;
    }
}
